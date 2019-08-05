package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.stripes.action.*;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableListActionBean;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This Action Bean takes Lab Batch or Lab Vessel Barcodes and generates XL20 pick lists by searching for the
 * tubes in the storage system.
 */
@UrlBinding(value = PickWorkspaceActionBean.ACTION_BEAN_URL)
public class PickWorkspaceActionBean extends CoreActionBean {

    protected static final Log log = LogFactory.getLog(ConfigurableListActionBean.class);
    public static final String ACTION_BEAN_URL = "/storage/pickWorkspace.action";

    // Events
    private static final String EVT_INIT = "init";
    private static final String EVT_PROCESS_BATCHES = "processBatches";
    private static final String EVT_DOWNLOAD_XFER_FILE = "buildXferFile";
    private static final String EVT_BULK_CHECKOUT = "processBulkCheckOut";
    private static final String EVT_CLOSE_BATCHES = "closeBatches";
    // UI Resolutions
    private static final String UI_DEFAULT = "/storage/picklist_workspace.jsp";
    private static final String UI_BULK_CHECKOUT = "/storage/bulk_checkout.jsp";

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private StorageEjb storageEjb;

    /**
     * JSON display collections - default to empty
     */
    private List<BatchSelectionData> batchSelectionList = Collections.EMPTY_LIST;
    private List<PickerDataRow> pickerDataRows = Collections.EMPTY_LIST;

    /* These come in from UI but aren't used */
    //private String tubesPerRack;
    //private boolean splitRacks;
    //private String[] targetRack;

    /**
     * Initial landing - user needs to select or create an existing workspace
     */
    @DefaultHandler
    @HandlesEvent(EVT_INIT)
    public Resolution eventInit(){
        // Initially empty
        pickerDataRows = new ArrayList<>();

        batchSelectionList = new ArrayList<>();
        for( LabBatch batch :  labBatchDao.findActiveByType(LabBatch.LabBatchType.SRS) ) {
            batchSelectionList.add( new BatchSelectionData(batch.getLabBatchId(), batch.getBatchName(),false,false) );
        }
        return new ForwardResolution(UI_DEFAULT);
    }

    /**
     * User wants to forward list of racks/ loose vessels to bulk checkout page
     */
    @HandlesEvent(EVT_BULK_CHECKOUT)
    public Resolution eventBulkCheckOut(){
        if( pickerDataRows.isEmpty() ){
            addGlobalValidationError("No batch vessels to check out");
            return new ForwardResolution(UI_DEFAULT);
        }

        String[] barcodes = new String[pickerDataRows.size()];
        for( int i = 0; i < pickerDataRows.size(); i++){
            barcodes[i] = pickerDataRows.get(i).sourceVessel;
        }

        ForwardResolution resolution = new ForwardResolution( BulkStorageOpsActionBean.class, BulkStorageOpsActionBean.EVT_INIT_CHECK_OUT );
        resolution.addParameter("checkOuts", barcodes);
        return resolution;
    }

    /**
     * Add or remove SRS batch vessels based upon user selections
     */
    @HandlesEvent(EVT_PROCESS_BATCHES)
    public Resolution eventProcessBatches(){

        List<LabBatch> batchListToAdd = new ArrayList<>();
        Set<Long> batchIdsToRemove = new HashSet<>();
        for( BatchSelectionData batchSelectionData :  batchSelectionList ) {
            // Data changed?
            if( batchSelectionData.wasSelected != batchSelectionData.isSelected ) {
                if( batchSelectionData.isSelected ) {
                    LabBatch thebatch = labBatchDao.findById(LabBatch.class, batchSelectionData.batchId);
                    if( thebatch == null ) {
                        addMessage(batchSelectionData.batchName + " not found." );
                    } else {
                        addMessage(batchSelectionData.batchName + " added");
                        batchListToAdd.add(thebatch);
                    }
                } else {
                    addMessage(batchSelectionData.batchName + " removed" );
                    batchIdsToRemove.add(batchSelectionData.batchId);
                }
                batchSelectionData.wasSelected = batchSelectionData.isSelected;
            }
        }

        // Remove deselected batches
        if( !batchIdsToRemove.isEmpty()) {
            pickerDataRows = pickerDataRows.stream()
                    .filter(row -> !batchIdsToRemove.contains(row.getBatchId()))
                    .collect(Collectors.toList());
        }

        // Reset any targets that may have been assigned on the client side, a change hoses the layout
        for( PickerDataRow row : pickerDataRows ) {
            row.setTargetRack(null);
            for( PickerVessel pickerVessel : row.getPickerVessels()) {
                pickerVessel.setTargetPosition(null);
                pickerVessel.setTargetVessel(null);
            }
        }

        for( LabBatch batchToAdd : batchListToAdd ) {
            pickerDataRows.addAll(getDataRowsForBatch(batchToAdd));
        }

        if( pickerDataRows.isEmpty()) {
            addMessage("No batches selected.");
        }

        return new ForwardResolution(UI_DEFAULT);
    }

    /**
     * Downloads the CSV robot transfer file
     */
    @HandlesEvent(EVT_DOWNLOAD_XFER_FILE)
    public Resolution buildTransferFile(){

        // These two for collecting conflicts
        Map<String,List<String>> tubeToBatches = new HashMap();
        Map<String,String> tubeToRack = new HashMap();

        // This for collection source-destination picker file - 5 element array:
        // { sourceRack, sourcePosition, sourceTube, targetRack, targetPosition }
        List<String[]> sourceTargetPickList = new ArrayList<>();

        if( pickerDataRows == null || pickerDataRows.isEmpty() ){
            addGlobalValidationError("Cannot continue - No vessels selected");
        } else {
            // Validations should be OK, it's also done pre-submit on client side
            batch_rack:
            for (PickerDataRow row : pickerDataRows) {
                if (!row.getRackScannable()) {
                    continue;
                }
                String sourceRackLabel = row.getSourceVessel();
                for (PickerVessel vessel : row.getPickerVessels()) {
                    String sourcePosition = vessel.getSourcePosition();
                    String sourceTubeLabel = vessel.getSourceVessel();
                    String targetRackLabel = vessel.getTargetVessel();
                    String targetPosition = vessel.getTargetPosition();

                    sourceTargetPickList.add( new String[]{ sourceRackLabel, sourcePosition, sourceTubeLabel, targetRackLabel, targetPosition } );

                    if (targetRackLabel == null || targetRackLabel.trim().isEmpty() || targetRackLabel.startsWith("DEST")
                            || targetPosition == null || targetPosition.trim().isEmpty() ) {
                        addGlobalValidationError("Cannot continue - Target rack barcodes are unassigned");
                        break batch_rack;
                    }
                    String tubeBarcode = vessel.getSourceVessel();
                    if( tubeToBatches.containsKey(tubeBarcode) ) {
                        tubeToBatches.get(tubeBarcode).add(row.getBatchName());
                    } else {
                        tubeToRack.put(tubeBarcode, row.getSourceVessel());
                        List<String> batchList = new ArrayList<>();
                        batchList.add(row.getBatchName());
                        tubeToBatches.put(tubeBarcode,batchList);
                    }
                }
            }
        }

        if( getContext().getValidationErrors().isEmpty() ) {
            byte[] comma = ",".getBytes();
            byte[] crlf = {0x0D, 0x0A};
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                for( String[] srcTarg : sourceTargetPickList ) {
                    out.write(srcTarg[0].getBytes());
                    out.write(comma);
                    out.write(srcTarg[1].getBytes());
                    out.write(comma);
                    out.write(srcTarg[2].getBytes());
                    out.write(comma);
                    out.write(srcTarg[3].getBytes());
                    out.write(comma);
                    out.write(srcTarg[4].getBytes());
                    out.write(crlf);
                }
            } catch (IOException ioEx) {
                log.error("Failed to create download", ioEx);
                addGlobalValidationError("Cannot continue - Failure creating download: " + ioEx.getMessage() );
            }

            if( getContext().getValidationErrors().isEmpty() ) {
                setFileDownloadHeaders("application/octet-stream", "SRS_Pick.csv");
                StreamingResolution stream = new StreamingResolution("application/octet-stream",
                        new ByteArrayInputStream(out.toByteArray()));
                return stream;
            }
        }

        // Still here means validation errors
        /* ******** See https://github.com/johnculviner/jquery.fileDownload to do this via jQuery ajax *****
        ValidationErrors errs = getContext().getValidationErrors();
        StringBuilder msg = new StringBuilder();
        msg.append("<p>");
        for( ValidationError err : errs.get(ValidationErrors.GLOBAL_ERROR) ){
            msg.append(err.getMessage(Locale.getDefault())).append("<br/>");
        }
        msg.append("</p>");
        StreamingResolution stream = new StreamingResolution("text/html",
                msg.toString());
        return stream;
        ** for now just recycle to batch page with errors   */

        return new ForwardResolution(UI_DEFAULT);
    }

    /**
     * Close (inactivate) selected batches and register layout of depleted racks in an InPlace event
     */
    @HandlesEvent(EVT_CLOSE_BATCHES)
    public Resolution eventCloseBatches() {
        Map<Long,LabBatch> batches = new HashMap<>();
        Map<RackOfTubes,TubeFormation> racksAndLayouts = new HashMap<>();
        for( PickerDataRow row : pickerDataRows ) {
            Long batchId = row.getBatchId();
            if( !batches.containsKey(batchId) ) {
                batches.put( batchId, labBatchDao.findById( LabBatch.class, batchId ) );
            }
            String srcBarcode = row.getSourceVessel();
            LabVessel src = labBatchDao.findSingleSafely(LabVessel.class, LabVessel_.label, srcBarcode, LockModeType.NONE);
            // Only create InPlace to register rack layout
            if( OrmUtil.proxySafeIsInstance( src, RackOfTubes.class ) ) {
                RackOfTubes rack = OrmUtil.proxySafeCast( src, RackOfTubes.class );
                TubeFormation priorTubeLayout = null;
                TubeFormation newTubeLayout = null;
                TreeSet<LabEvent> sortedEvents = new TreeSet<>( LabEvent.BY_EVENT_DATE );
                sortedEvents.addAll( rack.getAncillaryInPlaceEvents() );
                // Preferably last is a check-out as part of this pick or an old check-in, otherwise, force a scan
                LabEvent evt = sortedEvents.last();
                if( evt != null ) {
                    LabEventType evtType = evt.getLabEventType();
                    if( evtType == LabEventType.STORAGE_CHECK_IN || evtType == LabEventType.STORAGE_CHECK_OUT ) {
                        // Something registered layout, use it.  Always a tube formation when ancillary is a rack of tubes
                        priorTubeLayout = OrmUtil.proxySafeCast( evt.getInPlaceLabVessel(), TubeFormation.class );
                        newTubeLayout = removePicksFromLayout( priorTubeLayout, row.getPickerVessels() );
                        racksAndLayouts.put( rack, newTubeLayout );
                        break;
                    }
                } else {
                    addMessage("Cannot determine layout for rack " + rack.getLabel() + " - check-in will require a scan.");
                }
            }
        }

        long count = 0;
        Date now = new Date();
        Long userId = getUserBean().getBspUser().getUserId();
        for( Map.Entry<RackOfTubes,TubeFormation> rackAndLayout : racksAndLayouts.entrySet() ) {
            RackOfTubes rack = rackAndLayout.getKey();
            LabVessel newTubeFormation = rackAndLayout.getValue();
            newTubeFormation = storageEjb.tryPersistRackOrTubes(newTubeFormation);
            LabEvent rackLayoutRegistrationEvent = storageEjb.createDisambiguatedStorageEvent(
                    LabEventType.IN_PLACE, newTubeFormation, rack.getStorageLocation(), rack, userId, now, ++count );
            storageLocationDao.persist( rackLayoutRegistrationEvent );
            addMessage("New layout for rack " + rack.getLabel() + " recorded.");
        }
        for( LabBatch batch : batches.values() ) {
            batch.setActive(false);
            addMessage("Batch " + batch.getBatchName() + " set to inactive.");
        }
        storageLocationDao.flush();

        // Start from clean slate - remove inactive batches
        return eventInit();
    }

    /**
     * Builds a new tube formation based upon removing picked vessel from prior tube formation <br/>
     * Does no persistence existence check
     */
    private TubeFormation removePicksFromLayout(TubeFormation priorTubeFormation, List<PickerVessel> pickerVessels) {
        Set<String> barcodesToRemove = new HashSet<>();
        for( PickerVessel pick : pickerVessels ) {
            barcodesToRemove.add( pick.getSourceVessel() );
        }
        Map<VesselPosition,BarcodedTube> newLayoutMap = new HashMap<>();
        for( Map.Entry<VesselPosition,BarcodedTube> priorLayoutEntry : priorTubeFormation.getContainerRole().getMapPositionToVessel().entrySet() ) {
            if( !barcodesToRemove.contains(priorLayoutEntry.getValue().getLabel() ) ) {
                newLayoutMap.put( priorLayoutEntry.getKey(), priorLayoutEntry.getValue() );
            }
        }
        return new TubeFormation(newLayoutMap, priorTubeFormation.getRackType());
    }


    /**
     * Refreshes list of SRS batches and merges with existing
     */
    public void refreshSrsBatchList() {
        // TODO: implement
    }

    /**
     * State changes of batchSelectionList is posted back as JSON
     */
    public void setBatchSelectionList( String batchSelectionListJson ) {
        // Empty list is default
        batchSelectionList = new ArrayList<>();
        if( batchSelectionListJson == null || batchSelectionListJson.isEmpty() ) {
            return;
        }
        // De-serialize post-back
        ObjectMapper mapper = new ObjectMapper();
        try {
            batchSelectionList = mapper.readValue(batchSelectionListJson, new TypeReference<List<BatchSelectionData>>(){});
        } catch (IOException e) {
            addGlobalValidationError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Output batchSelectionList as JSON
     */
    public String getBatchSelectionList() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writer().writeValueAsString(batchSelectionList);
    }

    /**
     * Output container data list as JSON
     */
    public String getPickerData() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writer().writeValueAsString(pickerDataRows);
        } catch (IOException e) {
            log.error( e.getMessage(), e );
            // TODO Implement JSON error handling
            return "{}";
        }
    }

    /**
     * State changes of picker data table is posted back as JSON
     */
    public void setPickerData( String pickerDataJson ) {
        // Empty list is default
        pickerDataRows = new ArrayList<>();
        if( pickerDataJson == null || pickerDataJson.isEmpty() ) {
            return;
        }
        // De-serialize post-back
        ObjectMapper mapper = new ObjectMapper();
        try {
            pickerDataRows = mapper.readValue(pickerDataJson, new TypeReference<List<PickerDataRow>>(){});
        } catch (IOException e) {
            addGlobalValidationError(e.getMessage());
            e.printStackTrace();
        }
    }

    public int getRackCount(){
        Set<String> racks = new HashSet<>();
        for( PickerDataRow row : pickerDataRows ) {
            racks.add(row.getSourceVessel());
        }
        return racks.size();
    }

    public int getPickSampleCount(){
        int count = 0;
        for( PickerDataRow row : pickerDataRows ) {
            count += row.getSrsVesselCount();
        }
        return count;
    }

    /**
     * The money method: given an SRS batch, aggregate all the associated racks-->tubes-->storage locations <br/>
     * Also handle loose vessels
     */
    private List<PickerDataRow> getDataRowsForBatch(LabBatch batchToAdd) {
        Set<LabVessel> batchVessels = batchToAdd.getNonReworkStartingLabVessels();
        if( batchVessels.isEmpty() ) {
            return Collections.EMPTY_LIST;
        }

        // Track container vessel and associated picker row
        Map<LabVessel, PickerDataRow> containerRows = new HashMap<>();

        for(LabVessel batchVessel : batchVessels ) {
            StorageLocation storageLocation = batchVessel.getStorageLocation();
            PickerDataRow pickerDataRow;

            if( storageLocation != null && storageLocation.getLocationType() == StorageLocation.LocationType.LOOSE ) {
                pickerDataRow = buildDataRowForLooseVessel( batchToAdd, batchVessel );
                containerRows.put( batchVessel, pickerDataRow );
            } else if( OrmUtil.proxySafeIsInstance( batchVessel, PlateWell.class ) ) {
                PlateWell well = OrmUtil.proxySafeCast( batchVessel, PlateWell.class );
                StaticPlate plate = well.getPlate();
                pickerDataRow = containerRows.get( plate );
                if( pickerDataRow == null ) {
                    pickerDataRow = buildDataRowForPlateWell( batchToAdd, well );
                    containerRows.put( plate, pickerDataRow );
                }
                PickerVessel pickerVessel = new PickerVessel(plate.getLabel(), well.getVesselPosition().name());
                pickerDataRow.getPickerVessels().add(pickerVessel);
            } else {
                // Much uglier - racks and tubes
                Triple<LabVessel, TubeFormation, LabEvent> vesselStorageData = findVesselAndLocation(batchVessel);
                if( vesselStorageData == null ) {
                    continue;
                }
                LabVessel rackOrPlate = vesselStorageData.getLeft();
                TubeFormation storedTubeFormation = vesselStorageData.getMiddle();
                LabEvent storageEvent = vesselStorageData.getRight();
                if (containerRows.containsKey(rackOrPlate)) {
                    pickerDataRow = containerRows.get(rackOrPlate);
                } else {
                    pickerDataRow = new PickerDataRow(batchToAdd.getLabBatchId(), batchToAdd.getBatchName());
                    pickerDataRow.setSourceVessel(rackOrPlate.getLabel());
                    RackOfTubes rack = OrmUtil.proxySafeCast(rackOrPlate, RackOfTubes.class);
                    pickerDataRow.setRackScannable(rack.getRackType().isRackScannable()
                            && rack.getRackType().name().startsWith("Matrix"));
                    if (storageLocation != null) {
                        Long storageLocationId = storageLocation.getStorageLocationId();
                        pickerDataRow.setStorageLocId(storageLocationId);
                        pickerDataRow.setStorageLocPath(storageLocationDao.getLocationTrail(storageLocation));
                    } else {
                        if( storageEvent == null || storageEvent.getLabEventType() != LabEventType.STORAGE_CHECK_OUT ) {
                            pickerDataRow.setStorageLocPath("(Not in Storage)");
                        } else {
                            storageLocation = storageEvent.getStorageLocation();
                            if( storageLocation != null ) {
                                pickerDataRow.setStorageLocPath("Checked out of " + storageLocation.buildLocationTrail() + " on " + SimpleDateFormat.getDateInstance().format(storageEvent.getEventDate()));
                            } else {
                                // Pre GPLIM-5728 will not have event storage locations
                                pickerDataRow.setStorageLocPath("Checked out on " + SimpleDateFormat.getDateInstance().format(storageEvent.getEventDate()));
                            }
                        }
                    }
                    if (storedTubeFormation != null) {
                        // Total sample count value - only need to do it once
                        int vesselCount = storedTubeFormation.getContainerRole().getContainedVessels().size();
                        // Trust latest layout - assumes a checkout was done before a check-in
                        // for (LabVessel tube : storedTubeFormation.getContainerRole().getContainedVessels()) {
                        //     if (tube.getStorageLocation() != null && tube.getStorageLocation().getStorageLocationId().equals(storageLocationId)) {
                        //         vesselCount++;
                        //     }
                        // }
                        pickerDataRow.setTotalVesselCount(vesselCount);
                    }
                    containerRows.put(rackOrPlate, pickerDataRow);
                }  // End logic to build pickerDataRow - only done once

                // For tube formations, build out the source lab vessel label and position
                VesselPosition position = storedTubeFormation.getContainerRole().getPositionOfVessel(batchVessel);
                PickerVessel pickerVessel = new PickerVessel(batchVessel.getLabel(), position.name());
                pickerDataRow.getPickerVessels().add(pickerVessel);
            }
        }

        return new ArrayList( containerRows.values() );
    }

    /**
     * Build PickerDataRow for vessel stored in a LOOSE location
     */
    private PickerDataRow buildDataRowForLooseVessel( LabBatch batchToAdd, LabVessel looseVessel ){
        StorageLocation storageLocation = looseVessel.getStorageLocation();
        PickerDataRow pickerDataRow = new PickerDataRow(batchToAdd.getLabBatchId(), batchToAdd.getBatchName());
        pickerDataRow.setSourceVessel( looseVessel.getLabel() );
        pickerDataRow.setRackScannable(false);
        pickerDataRow.setTotalVesselCount(1);
        PickerVessel pickerVessel = new PickerVessel(looseVessel.getLabel(), "");
        pickerDataRow.getPickerVessels().add(pickerVessel);
        pickerDataRow.setStorageLocId( storageLocation.getStorageLocationId() );
        pickerDataRow.setStorageLocPath( storageLocation.buildLocationTrail() );
        return pickerDataRow;
    }

    /**
     * Build PickerDataRow for vessel stored in a LOOSE location
     */
    private PickerDataRow buildDataRowForPlateWell( LabBatch batchToAdd, PlateWell well ){
        StaticPlate plate = well.getPlate();
        StorageLocation storageLocation = plate.getStorageLocation();
        PickerDataRow pickerDataRow = new PickerDataRow(batchToAdd.getLabBatchId(), batchToAdd.getBatchName());
        pickerDataRow.setSourceVessel( plate.getLabel() );
        pickerDataRow.setRackScannable(false);
        pickerDataRow.setTotalVesselCount(plate.getGeometrySize());
        if( storageLocation != null ) {
            pickerDataRow.setStorageLocId( storageLocation.getStorageLocationId() );
            pickerDataRow.setStorageLocPath( storageLocation.buildLocationTrail() );
        } else {
            LabEvent latestStorageEvent = plate.getLatestStorageEvent();
            if( latestStorageEvent != null && latestStorageEvent.getLabEventType() == LabEventType.STORAGE_CHECK_OUT ) {
                storageLocation = latestStorageEvent.getStorageLocation();
                if( storageLocation != null ) {
                    pickerDataRow.setStorageLocPath("Checked out of " + storageLocation.buildLocationTrail() + " on " + SimpleDateFormat.getDateInstance().format(latestStorageEvent.getEventDate()));
                } else {
                    // Pre GPLIM-5728 will not have event storage locations
                    pickerDataRow.setStorageLocPath("Checked out on " + SimpleDateFormat.getDateInstance().format(latestStorageEvent.getEventDate()));
                }
            } else {
                pickerDataRow.setStorageLocPath( "(Not in storage)" );
            }
        }

        return pickerDataRow;
    }

    /**
     * Find rack and location for a tube
     */
    private Triple<LabVessel, TubeFormation, LabEvent> findVesselAndLocation( LabVessel tube ) {

        LabEvent latestCheckInEvent;
        LabEvent latestCheckOutEvent;
        LabEvent latestStorageEvent = tube.getLatestStorageEvent();
        if( latestStorageEvent != null && latestStorageEvent.getLabEventType() == LabEventType.STORAGE_CHECK_OUT ) {
            latestCheckInEvent = null;
            latestCheckOutEvent = latestStorageEvent;
        } else {
            latestCheckInEvent = latestStorageEvent;
            latestCheckOutEvent = null;
        }

        // Most are 1:1 rack to tube formation
        TubeFormation singleTubeFormation = null;
        if( latestCheckInEvent != null ) {
            if( OrmUtil.proxySafeIsInstance(latestCheckInEvent.getInPlaceLabVessel(), TubeFormation.class) ) {
                singleTubeFormation = OrmUtil.proxySafeCast(latestCheckInEvent.getInPlaceLabVessel(), TubeFormation.class);
            } else {
                singleTubeFormation = null;
            }
            return Triple.of(latestCheckInEvent.getAncillaryInPlaceVessel(), singleTubeFormation, latestCheckInEvent);
        } else if( latestCheckOutEvent != null ) {
            if (OrmUtil.proxySafeIsInstance(latestCheckOutEvent.getInPlaceLabVessel(), TubeFormation.class)) {
                singleTubeFormation = OrmUtil.proxySafeCast(latestCheckOutEvent.getInPlaceLabVessel(), TubeFormation.class);
            } else {
                singleTubeFormation = null;
            }
            return Triple.of(latestCheckOutEvent.getAncillaryInPlaceVessel(), singleTubeFormation, latestCheckOutEvent);
        } else {
            TreeSet<LabEvent> sortedEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);
            // Which event owns which tube formation?
            Map<LabEvent,TubeFormation> eventTubes = new HashMap<>();
            for (LabVessel tubeContainer : tube.getContainers() ) {
                if( OrmUtil.proxySafeIsInstance( tubeContainer, TubeFormation.class ) ) {
                    TubeFormation formation = OrmUtil.proxySafeCast( tubeContainer, TubeFormation.class );
                    for( LabEvent evt : formation.getTransfersTo() ) {
                        sortedEvents.add(evt);
                        eventTubes.put(evt,formation);
                    }
                    for( LabEvent evt : formation.getTransfersFrom() ) {
                        sortedEvents.add(evt);
                        eventTubes.put(evt,formation);
                    }
                }
            }
            if (sortedEvents.size() > 0) {
                LabEvent latestRackEvent = sortedEvents.last();
                return Triple.of(latestRackEvent.getAncillaryInPlaceVessel(), OrmUtil.proxySafeCast( latestRackEvent.getInPlaceLabVessel(), TubeFormation.class ), sortedEvents.last());
            } else {
                return null;
            }
        }

    }

    public static class BatchSelectionData {
        Long batchId;
        String batchName;
        boolean wasSelected;
        boolean isSelected;

        // JSON
        BatchSelectionData(){}

        BatchSelectionData(Long batchId, String batchName, boolean wasSelected, boolean isSelected){
            this.batchId = batchId;
            this.batchName = batchName;
            this.wasSelected = wasSelected;
            this.isSelected = isSelected;
        }

        public Long getBatchId() {
            return batchId;
        }

        public void setBatchId(Long batchId) {
            this.batchId = batchId;
        }

        public String getBatchName() {
            return batchName;
        }

        public void setBatchName(String batchName) {
            this.batchName = batchName;
        }

        public boolean isWasSelected() {
            return wasSelected;
        }

        public void setWasSelected(boolean wasSelected) {
            this.wasSelected = wasSelected;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }

    public static class PickerDataRow {
        Long batchId;
        String batchName;
        Long storageLocId;
        String storageLocPath;
        // RackOfTubes, StaticPlate, or BarcodedTube if loose
        String sourceVessel;
        // Contained vessel barcodes if applicable
        List<PickerVessel> pickerVessels;
        String targetRack;
        String targetRackName;
        int totalVesselCount;
        // Ability to pick on XL20
        boolean rackScannable = true;

        // JSON
        PickerDataRow(){}

        PickerDataRow(Long batchId, String batchName){
            this.batchId = batchId;
            this.batchName = batchName;
        }

        public Long getBatchId() {
            return batchId;
        }

        public void setBatchId(Long batchId) {
            this.batchId = batchId;
        }

        public String getBatchName() {
            return batchName;
        }

        public void setBatchName(String batchName) {
            this.batchName = batchName;
        }

        public Long getStorageLocId() {
            return storageLocId;
        }

        public void setStorageLocId(Long storageLocId) {
            this.storageLocId = storageLocId;
        }

        public String getStorageLocPath() {
            return storageLocPath;
        }

        public void setStorageLocPath(String storageLocPath) {
            this.storageLocPath = storageLocPath;
        }

        public String getSourceVessel() {
            return sourceVessel;
        }

        public void setSourceVessel(String sourceVessel) {
            this.sourceVessel = sourceVessel;
        }

        public List<PickerVessel> getPickerVessels() {
            if( pickerVessels == null ) {
                pickerVessels = new ArrayList<>();
            }
            return pickerVessels;
        }

        public void setTubeBarcodes(List<PickerVessel> pickerVessels) {
            this.pickerVessels = pickerVessels;
        }

        public String getTargetRack() {
            return targetRack;
        }

        public void setTargetRack(String targetRack) {
            this.targetRack = targetRack;
        }

        public String getTargetRackName() {
            return targetRackName;
        }

        public void setTargetRackName(String targetRackName) {
            this.targetRackName = targetRackName;
        }

        public int getTotalVesselCount() {
            return totalVesselCount;
        }

        public void setTotalVesselCount(int totalVesselCount) {
            this.totalVesselCount = totalVesselCount;
        }

        public int getSrsVesselCount() {
            return getPickerVessels().size();
        }

        /**
         * JSON needs this to unmarshall, but value is read from tube list length
         **/
        public void setSrsVesselCount(int srsVesselCount) {
        }


        public boolean getRackScannable() {
            return rackScannable;
        }

        public void setRackScannable(boolean rackScannable) {
            this.rackScannable = rackScannable;
        }
    }

    public static class PickerVessel {
        // BarcodedTube
        String sourceVessel;
        String sourcePosition;
        String targetVessel;
        String targetPosition;

        // JSON
        PickerVessel() {}

        PickerVessel(String sourceVessel, String sourcePosition) {
            this.sourceVessel = sourceVessel;
            this.sourcePosition = sourcePosition;
        }

        public String getSourceVessel() {
            return sourceVessel;
        }

        public void setSourceVessel(String sourceVessel) {
            this.sourceVessel = sourceVessel;
        }

        public String getSourcePosition() {
            return sourcePosition;
        }

        public void setSourcePosition(String sourcePosition) {
            this.sourcePosition = sourcePosition;
        }

        public String getTargetVessel() {
            return targetVessel;
        }

        public void setTargetVessel(String targetVessel) {
            this.targetVessel = targetVessel;
        }

        public String getTargetPosition() {
            return targetPosition;
        }

        public void setTargetPosition(String targetPosition) {
            this.targetPosition = targetPosition;
        }
    }

}
