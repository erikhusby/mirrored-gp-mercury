package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.*;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This Action Bean takes Lab Batch or Lab Vessel Barcodes and generates XL20 pick lists by searching for the
 * tubes in the storage system.
 */
@UrlBinding(value = PickWorkspaceActionBean.ACTION_BEAN_URL)
public class PickWorkspaceActionBean extends CoreActionBean {


    private static final Logger logger = Logger.getLogger(PickWorkspaceActionBean.class.getName());
    public static final String ACTION_BEAN_URL = "/vessel/pickWorkspace.action";

    // Events
    private static final String EVT_INIT = "init";
    private static final String EVT_PROCESS_BATCHES = "processBatches";

    // UI Resolutions
    private static final String UI_DEFAULT = "/storage/picklist_workspace.jsp";

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabEventDao labEventDao;

    /**
     * JSON display collections - default to empty
     */
    private List<BatchSelectionData> batchSelectionList = Collections.EMPTY_LIST;
    private List<PickerDataRow> pickerDataRows = Collections.EMPTY_LIST;

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
     * Add or remove SRS batch vessels
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

        // TODO:  This is actually do-able on the client side, but just because something can be done, should it be done?
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

        return new ForwardResolution(UI_DEFAULT);

        //return new StreamingResolution("text/json", getPickerData());
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
            logger.log(Level.SEVERE, e.getMessage(), e);
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

    /**
     * Given an SRS batch, aggregate all the associated racks-->tubes-->storage locations <br/>
     * Also handle loose vessels
     */
    private List<PickerDataRow> getDataRowsForBatch(LabBatch batchToAdd) {
        /* ****************************                                                            ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  MONSTERS ALL BE HERE********************************************** */
        Set<LabVessel> batchVessels = batchToAdd.getNonReworkStartingLabVessels();
        if( batchVessels.isEmpty() ) {
            return Collections.EMPTY_LIST;
        }

        // Track container vessel and associated picker row
        Map<LabVessel, PickerDataRow> containerRows = new HashMap<>();

        for(LabVessel batchVessel : batchVessels ) {
            Triple<LabVessel,TubeFormation,StorageLocation> containerLoc = findVesselAndLocation( batchVessel );
            LabVessel rackOrLooseTube = containerLoc.getLeft();
            TubeFormation storedTubeFormation = containerLoc.getMiddle();
            StorageLocation storageLocation = containerLoc.getRight();
            PickerDataRow pickerDataRow;
            if( containerRows.containsKey( rackOrLooseTube ) ) {
                pickerDataRow = containerRows.get(rackOrLooseTube);
            } else {
                pickerDataRow = new PickerDataRow(batchToAdd.getLabBatchId(), batchToAdd.getBatchName());
                pickerDataRow.setSourceVessel( rackOrLooseTube.getLabel() );
                if( OrmUtil.proxySafeIsInstance(rackOrLooseTube, RackOfTubes.class) ) {
                    RackOfTubes rack = OrmUtil.proxySafeCast(rackOrLooseTube, RackOfTubes.class);
                    pickerDataRow.setRackScannable( rack.getRackType().isRackScannable()
                        && rack.getRackType().name().startsWith("Matrix"));
                } else {
                    pickerDataRow.setRackScannable(false);
                }
                if( storageLocation != null ) {
                    Long storageLocationId = storageLocation.getStorageLocationId();
                    pickerDataRow.setStorageLocId( storageLocationId );
                    pickerDataRow.setStorageLocPath(storageLocationDao.getLocationTrail(storageLocationId));
                    if( storedTubeFormation != null ) {

                        // Total sample count value - only need to do it once
                        int vesselCount = 0;
                        for (LabVessel tube : storedTubeFormation.getContainerRole().getContainedVessels()) {
                            if (tube.getStorageLocation() != null && tube.getStorageLocation().getStorageLocationId().equals(storageLocationId)) {
                                vesselCount++;
                            }
                        }
                        pickerDataRow.setTotalVesselCount(vesselCount);
                    } else {
                        // Loose
                        pickerDataRow.setTotalVesselCount(1);
                    }
                } else {
                    pickerDataRow.setStorageLocPath("(Not in Storage)");
                }
                containerRows.put( rackOrLooseTube, pickerDataRow );
            }

            // For tube formations, build out the source lab vessel label and position
            if( storedTubeFormation != null ) {
                VesselPosition position = storedTubeFormation.getContainerRole().getPositionOfVessel(batchVessel);
                PickerVessel pickerVessel = new PickerVessel(batchVessel.getLabel(), position.name());
                pickerDataRow.getPickerVessels().add(pickerVessel);
            }
        }

        return new ArrayList( containerRows.values() );
    }

    /**
     * Find rack and location for a tube (or well?) </br>
     * If loose location, return the original vessel and location
     */
    private Triple<LabVessel, TubeFormation, StorageLocation> findVesselAndLocation(LabVessel tube ) {

        StorageLocation tubeLocation = tube.getStorageLocation();
        if( tubeLocation == null ) {
            return null;
        } else if( tubeLocation.getLocationType() == StorageLocation.LocationType.LOOSE ) {
            return Triple.of( tube, null, tubeLocation );
        } else if( OrmUtil.proxySafeIsInstance( tube, PlateWell.class ) ) {
            return Triple.of( OrmUtil.proxySafeCast( tube, PlateWell.class ).getPlate(), null, tubeLocation );
        }
        LabEvent latestCheckInEvent;
        LabEvent latestStorageEvent = tube.getLatestStorageEvent();
        // Ignore check-out
        if( latestStorageEvent != null && latestStorageEvent.getLabEventType() == LabEventType.STORAGE_CHECK_OUT ) {
            latestCheckInEvent = null;
        } else {
            latestCheckInEvent = latestStorageEvent;
        }

        // Most are 1:1 rack to tube formation
        TubeFormation singleTubeFormation = null;
        if( latestCheckInEvent != null ) {
            if( OrmUtil.proxySafeIsInstance(latestCheckInEvent.getInPlaceLabVessel(), TubeFormation.class) ) {
                singleTubeFormation = OrmUtil.proxySafeCast(latestCheckInEvent.getInPlaceLabVessel(), TubeFormation.class);
            } else {
                singleTubeFormation = null;
            }
            return Triple.of(latestCheckInEvent.getAncillaryInPlaceVessel(), singleTubeFormation, tubeLocation);
        } else {
            return null;
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
