package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableListActionBean;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;
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
    static final String EVT_INIT = "init";
    static final String EVT_PROCESS_BATCHES = "processBatches";
    static final String EVT_DOWNLOAD_XFER_FILE = "buildXferFile";
    static final String EVT_BULK_CHECKOUT = "processBulkCheckOut";
    static final String EVT_CLOSE_BATCHES = "closeBatches";
    static final String EVT_SHOW_PICK_VERIFY = "showVerifyPicks";
    static final String EVT_REGISTER_TRANSFERS = "registerTransfers";
    // UI Resolutions
    private static final String UI_DEFAULT = "/storage/picklist_workspace.jsp";
    private static final String UI_BULK_CHECKOUT = "/storage/bulk_checkout.jsp";
    private static final String UI_PICK_VERIFY = "/storage/srs_batch_verify.jsp";

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private StorageEjb storageEjb;

    /**
     * JSON display collections - default to empty
     */
    private List<BatchSelectionData> batchSelectionList = Collections.EMPTY_LIST;
    private List<PickerDataRow> pickerDataRows = Collections.EMPTY_LIST;

    private String scanDataJson;

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
        for (LabBatch batch : labBatchDao.findByTypeAndActiveStatus(LabBatch.LabBatchType.SRS, Boolean.TRUE)) {
            batchSelectionList.add(new BatchSelectionData(batch.getLabBatchId(), batch.getBatchName(), false, false, batch.getActive()));
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
        int selectedCount = 0;
        for( BatchSelectionData batchSelectionData :  batchSelectionList ) {
            if (batchSelectionData.isSelected) {
                selectedCount++;
            }

            // Data changed?
            if (batchSelectionData.wasSelected != batchSelectionData.isSelected) {
                if (batchSelectionData.isSelected) {
                    LabBatch thebatch = labBatchDao.findById(LabBatch.class, batchSelectionData.batchId);
                    if (thebatch == null) {
                        addMessage(batchSelectionData.batchName + " not found.");
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
            for (PickerVessel pickerVessel : row.getPickerVessels()) {
                pickerVessel.setTargetPosition(null);
                pickerVessel.setTargetVessel(null);
            }
        }

        for (LabBatch batchToAdd : batchListToAdd) {
            pickerDataRows.addAll(getDataRowsForBatch(batchToAdd));
        }

        if (pickerDataRows.isEmpty() && selectedCount > 0) {
            addMessage("No available batch vessels.");
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
            byte[] crlf = {0x0D, 0x0A}; // XL20 is a Windows interface
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
        ** but for now just recycle to batch page with errors   */

        return new ForwardResolution(UI_DEFAULT);
    }

    /**
     * Basically a redirect to allow user to verify all expected vessels are picked
     * Data table rebuilt from what's passed in
     */
    @HandlesEvent(EVT_SHOW_PICK_VERIFY)
    public Resolution showVerifyPicks() {
        return new ForwardResolution(UI_PICK_VERIFY);
    }

    /**
     * Registers the barcodes and layouts of all source and destination racks after picking
     */
    @HandlesEvent(EVT_REGISTER_TRANSFERS)
    public Resolution registerTransfers() {
        if (scanDataJson == null) {
            addGlobalValidationError("No scan data provided");
            return new ForwardResolution(UI_PICK_VERIFY);
        }

        Long userId = userBean.getBspUser().getUserId();

        // Extract scan data from JSON
        Map<String, List<Pair<String, VesselPosition>>> destinationLayouts = new HashMap<>();
        Set<String> allDestBarcodes = new HashSet<>();
        boolean hadFailure = extractDestinationLayouts(destinationLayouts, allDestBarcodes);
        if (hadFailure) {
            return new ForwardResolution(UI_PICK_VERIFY);
        }

        // Create racks and formations without persisting (tubes MUST exist!)
        Map<RackOfTubes, TubeFormation> destRacksAndTubes = new HashMap<>();
        hadFailure = buildRacksAndFormations(destinationLayouts, destRacksAndTubes);
        if (hadFailure) {
            // Die - tubes did not exist
            return new ForwardResolution(UI_PICK_VERIFY);
        }

        // Build and persist racks, formations, and scan events for destinations
        int disambiguator = 0;
        Date now = new Date();
        for (Map.Entry<RackOfTubes, TubeFormation> rackAndFormation : destRacksAndTubes.entrySet()) {
            RackOfTubes rack = OrmUtil.proxySafeCast(
                    storageEjb.tryPersistRackOrTubes(rackAndFormation.getKey()), RackOfTubes.class);
            TubeFormation formation = OrmUtil.proxySafeCast(
                    storageEjb.tryPersistRackOrTubes(rackAndFormation.getValue()), TubeFormation.class);

            rack.getTubeFormations().add(formation);
            formation.getRacksOfTubes().add(rack);
            LabEvent inPlaceEvent = storageEjb.createDisambiguatedStorageEvent(LabEventType.IN_PLACE, formation, null, rack, userId, now, ++disambiguator);
            addMessage("Registered layout of destination rack " + rack.getLabel());
        }

        // Now remove tubes from source racks and register new layout
        Map<RackOfTubes, TubeFormation> srcRacksAndTubes = new HashMap<>();
        // All source racks
        for (PickerDataRow pickerDataRow : pickerDataRows) {
            LabVessel maybeRack = labVesselDao.findByIdentifier(pickerDataRow.sourceVessel);
            if (OrmUtil.proxySafeIsInstance(maybeRack, RackOfTubes.class)) {
                srcRacksAndTubes.put(OrmUtil.proxySafeCast(maybeRack, RackOfTubes.class), null);
            }
        }

        // Find tube formations of source racks based upon latest event
        for (Map.Entry<RackOfTubes, TubeFormation> mapEntry : srcRacksAndTubes.entrySet()) {
            RackOfTubes rack = mapEntry.getKey();
            SortedMap<LabEvent, TubeFormation> rackEvents = rack.getRackEventsSortedByDate();
            if (rackEvents.isEmpty()) {
                addMessage("No event for source rack " + rack.getLabel() + ", skipping source layout update!");
            } else {
                LabEvent latest = rackEvents.lastKey();
                if (storageEjb.isStorageEvent(latest)) {
                    mapEntry.setValue(rackEvents.get(latest));
                } else {
                    addMessage("Last event (" + latest.getLabEventType().getName() + ") for source rack " + rack.getLabel() + " is not storage related, skipping layout update!");
                }
            }
        }

        // Make a copy of prior layout map
        Map<RackOfTubes, Map<VesselPosition, BarcodedTube>> rackLayouts = new HashMap<>();
        for (Map.Entry<RackOfTubes, TubeFormation> mapEntry : srcRacksAndTubes.entrySet()) {
            RackOfTubes rack = mapEntry.getKey();
            if (mapEntry.getValue() != null) {
                rackLayouts.put(rack, new HashMap(mapEntry.getValue().getContainerRole().getMapPositionToVessel()));
            }
        }

        for (Map.Entry<RackOfTubes, Map<VesselPosition, BarcodedTube>> rackMapEntry : rackLayouts.entrySet()) {
            for (Iterator layoutIter = rackMapEntry.getValue().entrySet().iterator(); layoutIter.hasNext(); ) {
                Map.Entry<VesselPosition, BarcodedTube> layoutMap = (Map.Entry<VesselPosition, BarcodedTube>) layoutIter.next();
                if (allDestBarcodes.contains(layoutMap.getValue().getLabel())) {
                    layoutIter.remove();
                }
            }
        }

        // Build racks, formations, and scan events for destinations
        for (Map.Entry<RackOfTubes, Map<VesselPosition, BarcodedTube>> rackMapEntry : rackLayouts.entrySet()) {
            RackOfTubes rack = rackMapEntry.getKey();
            TubeFormation formation = new TubeFormation(rackMapEntry.getValue(), rack.getRackType());
            formation = OrmUtil.proxySafeCast(storageEjb.tryPersistRackOrTubes(formation), TubeFormation.class);

            rack.getTubeFormations().add(formation);
            formation.getRacksOfTubes().add(rack);
            LabEvent inPlaceEvent = storageEjb.createDisambiguatedStorageEvent(LabEventType.IN_PLACE, formation, null, rack, userId, now, ++disambiguator);
            addMessage("Registered layout of source rack " + rack.getLabel());
        }

        // Reset everything and return to pick workspace
        return eventInit();
    }

    /**
     * Close (inactivate) selected batches and register layout of depleted racks in an InPlace event
     */
    @HandlesEvent(EVT_CLOSE_BATCHES)
    public Resolution eventCloseBatches() {
        Map<Long, LabBatch> batches = new HashMap<>();
        Map<RackOfTubes, TubeFormation> racksAndLayouts = new HashMap<>();
        for (PickerDataRow row : pickerDataRows) {
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
                    if (storageEjb.isStorageEvent(evt)) {
                        // Something registered layout, use it.  Always a tube formation when ancillary is a rack of tubes
                        priorTubeLayout = OrmUtil.proxySafeCast(evt.getInPlaceLabVessel(), TubeFormation.class);
                        newTubeLayout = removePicksFromLayout(priorTubeLayout, row.getPickerVessels());
                        racksAndLayouts.put(rack, newTubeLayout);
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
     * Everything should exist - does no persistence existence check
     */
    private TubeFormation removePicksFromLayout(TubeFormation priorTubeFormation, List<PickerVessel> pickerVessels) {
        Set<String> barcodesToRemove = new HashSet<>();
        for( PickerVessel pick : pickerVessels ) {
            barcodesToRemove.add( pick.getSourceVessel() );
        }
        Map<VesselPosition, BarcodedTube> newLayoutMap = new HashMap<>();
        for (Map.Entry<VesselPosition, BarcodedTube> priorLayoutEntry : priorTubeFormation.getContainerRole().getMapPositionToVessel().entrySet()) {
            if (!barcodesToRemove.contains(priorLayoutEntry.getValue().getLabel())) {
                newLayoutMap.put(priorLayoutEntry.getKey(), priorLayoutEntry.getValue());
            }
        }
        return new TubeFormation(newLayoutMap, priorTubeFormation.getRackType());
    }

    /**
     * Extract pick destination layouts from scan JSON
     *
     * @return boolean for had failure - true if error, false if no error
     */
    private boolean extractDestinationLayouts(Map<String, List<Pair<String, VesselPosition>>> destinationLayouts,
                                              Set<String> allDestBarcodes) {
        boolean hadFailure = false;
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode root = null;
        try {
            root = (ArrayNode) mapper.readTree(scanDataJson);
            rackScanLoop:
            for (JsonNode rackScan : root) {
                String rackBarcode = rackScan.get("rackBarcode").textValue();
                if (StringUtils.isEmpty(rackBarcode)) {
                    hadFailure = true;
                    addGlobalValidationError("Rack barcode missing");
                    break rackScanLoop;
                } else if (destinationLayouts.containsKey(rackBarcode)) {
                    hadFailure = true;
                    addGlobalValidationError("Duplicate destination rack barcode: " + rackBarcode);
                    break rackScanLoop;
                }
                ArrayNode tubeScans = (ArrayNode) rackScan.get("scans");
                List<Pair<String, VesselPosition>> tubeLayout = new ArrayList<>();
                tubeLoop:
                for (JsonNode tubeScan : tubeScans) {
                    String barcode = tubeScan.get("barcode").textValue();
                    VesselPosition position = VesselPosition.valueOf(tubeScan.get("position").textValue());
                    tubeLayout.add(Pair.of(barcode, position));
                    allDestBarcodes.add(barcode);
                }
                destinationLayouts.put(rackBarcode, tubeLayout);
            }
        } catch (IOException e) {
            hadFailure = true;
            addGlobalValidationError(e.getMessage());
            e.printStackTrace();
        }
        return hadFailure;
    }

    /**
     * Build racks and tube formations without persisting (tubes MUST exist!)
     *
     * @return boolean for had failure - true if error, false if no error
     */
    private boolean buildRacksAndFormations(Map<String, List<Pair<String, VesselPosition>>> destinationLayouts,
                                            Map<RackOfTubes, TubeFormation> racksAndTubes) {
        boolean hadFailure = false;
        for (Map.Entry<String, List<Pair<String, VesselPosition>>> barcodeAndTubesEntry : destinationLayouts.entrySet()) {
            // Create rack
            RackOfTubes rack = new RackOfTubes(barcodeAndTubesEntry.getKey(), RackOfTubes.RackType.Matrix96);

            // Defend against scanning something weird
            LabVessel tryRack = labVesselDao.findByIdentifier(rack.getLabel());
            if (tryRack != null && !OrmUtil.proxySafeIsInstance(tryRack, RackOfTubes.class)) {
                hadFailure = true;
                addGlobalValidationError("Barcode " + rack.getLabel() + " exists but is not a rack.");
                continue;
            }

            Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
            for (Pair<String, VesselPosition> labelAndPos : barcodeAndTubesEntry.getValue()) {
                LabVessel tube = labVesselDao.findByIdentifier(labelAndPos.getLeft());
                if (tube == null) {
                    hadFailure = true;
                    addGlobalValidationError("Tube barcode " + labelAndPos.getLeft() + " not found.");
                    continue;
                } else {
                    if (OrmUtil.proxySafeIsInstance(tube, BarcodedTube.class)) {
                        mapPositionToTube.put(labelAndPos.getRight(), OrmUtil.proxySafeCast(tube, BarcodedTube.class));
                    } else {
                        hadFailure = true;
                        addGlobalValidationError("Barcode " + labelAndPos.getLeft() + " is not a tube.");
                        continue;
                    }
                }
            }
            TubeFormation tubeFormation = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
            racksAndTubes.put(rack, tubeFormation);
        }
        return hadFailure;
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
    public void setPickerData(String pickerDataJson ) {
        // Empty list is default
        pickerDataRows = new ArrayList<>();
        if( pickerDataJson == null || pickerDataJson.isEmpty() ) {
            return;
        }
        // De-serialize post-back
        ObjectMapper mapper = new ObjectMapper();
        try {
            pickerDataRows = mapper.readValue(pickerDataJson, new TypeReference<List<PickerDataRow>>() {
            });
        } catch (IOException e) {
            addGlobalValidationError(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Read only data containing list of rack scans posted as JSON
     */
    public void setScanData(String scanDataJson) {
        if (scanDataJson == null || scanDataJson.isEmpty()) {
            return;
        }
        this.scanDataJson = scanDataJson;
    }

    public int getRackCount() {
        Set<String> racks = new HashSet<>();
        for (PickerDataRow row : pickerDataRows) {
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
                Triple<LabVessel, TubeFormation, LabEvent> vesselStorageData = storageEjb.findLatestRackAndLayout(batchVessel);
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
            LabEvent latestStorageEvent = storageEjb.getLatestStorageEvent(plate);
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

    public static class BatchSelectionData {
        Long batchId;
        String batchName;
        boolean wasSelected;
        boolean isSelected;
        boolean isActive;

        // JSON
        BatchSelectionData(){}

        BatchSelectionData(Long batchId, String batchName, boolean wasSelected, boolean isSelected, boolean isActive) {
            this.batchId = batchId;
            this.batchName = batchName;
            this.wasSelected = wasSelected;
            this.isSelected = isSelected;
            this.isActive = isActive;
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

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
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
