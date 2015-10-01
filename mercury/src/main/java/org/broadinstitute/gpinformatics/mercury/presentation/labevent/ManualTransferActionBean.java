package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationSetupEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A Stripes Action Bean to record manual transfers.
 */
@UrlBinding(ManualTransferActionBean.ACTION_BEAN_URL)
public class ManualTransferActionBean extends RackScanActionBean {
    private static final Log log = LogFactory.getLog(ManualTransferActionBean.class);

    public static final String MANUAL_TRANSFER_PAGE = "/labevent/manual_transfer.jsp";
    public static final String CHOOSE_EVENT_TYPE_ACTION = "chooseEventType";
    public static final String TRANSFER_ACTION = "transfer";
    public static final String FETCH_EXISTING_ACTION = "fetchExisting";
    public static final String ACTION_BEAN_URL = "/labevent/manualtransfer.action";
    public static final String PAGE_TITLE = "Manual Transfers";
    public static final String RACK_SCAN_EVENT = "rackScan";

    /** Parameter from batch workflow page. */
    private String workflowProcessName;
    /** Parameter from batch workflow page. */
    private String workflowStepName;
    /** Parameter from batch workflow page. */
    private Date workflowEffectiveDate;
    /** Parameter from batch workflow page. */
    private String batchName;
    /** Parameter from batch workflow page, allows return to same link. */
    private String anchorName;
    /** Loaded based on parameters. */
    private WorkflowStepDef workflowStepDef;

    /** POSTed from the form. */
    private List<StationEventType> stationEvents = new ArrayList<>();
    /** POSTed from the form, for rack scan. */
    private Integer scanIndex;
    /** POSTed from the form, for rack scan. */
    private Boolean scanSource;
    /** Set in the init method, from a POSTed parameter. */
    private LabEventType labEventType;
    /** Set in the init method, from workflowStepDef or labEventType. */
    private LabEventType.ManualTransferDetails manualTransferDetails;
    /** Makes unique the synthetic barcodes of racks. */
    private int anonymousRackDisambiguator = 1;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private WorkflowLoader workflowLoader;

    @Inject
    private LabBatchDao labBatchDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    /**
     * Create StationEvent subclasses, so Stripes can bind form fields to correct structure.
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        String eventType = getContext().getRequest().getParameter("stationEvents[0].eventType");
        if (eventType != null) {
            labEventType = LabEventType.getByName(eventType);
            String workflowEffectiveDateLocal = getContext().getRequest().getParameter("workflowEffectiveDate");
            if (!StringUtils.isEmpty(workflowEffectiveDateLocal)) {
                workflowStepDef = loadWorkflowStepDef(new Date(workflowEffectiveDateLocal),
                        workflowLoader, getContext().getRequest().getParameter("workflowProcessName"),
                        getContext().getRequest().getParameter("workflowStepName"));
            }
            if (workflowStepDef == null) {
                manualTransferDetails = labEventType.getManualTransferDetails();
            } else {
                manualTransferDetails = workflowStepDef.getManualTransferDetails();
                if (manualTransferDetails == null) {
                    manualTransferDetails = labEventType.getManualTransferDetails();
                }
            }
            assert manualTransferDetails != null;

            // A secondary event (one source, two destinations) requires an additional station event
            int numEvents = manualTransferDetails.getNumEvents();
            if (manualTransferDetails.getSecondaryEvent() != null) {
                numEvents++;
            }

            for (int i = 0; i < numEvents; i++) {
                StationEventType stationEvent;
                switch (manualTransferDetails.getMessageType()) {
                    case PLATE_EVENT:
                        stationEvent = new PlateEventType();
                        break;
                    case PLATE_TRANSFER_EVENT:
                        stationEvent = new PlateTransferEventType();
                        break;
                    case STATION_SETUP_EVENT:
                        stationEvent = new StationSetupEvent();
                        break;
                    case PLATE_CHERRY_PICK_EVENT:
                        stationEvent = new PlateCherryPickEvent();
                        break;
                    case RECEPTACLE_PLATE_TRANSFER_EVENT:
                        stationEvent = new ReceptaclePlateTransferEvent();
                        break;
                    case RECEPTACLE_EVENT:
                        stationEvent = new ReceptacleEventType();
                        break;
                    case RECEPTACLE_TRANSFER_EVENT:
                        stationEvent = new ReceptacleTransferEventType();
                        break;
                    default:
                        throw new RuntimeException("Unknown labEventType " + manualTransferDetails.getMessageType());
                }
                stationEvents.add(stationEvent);
            }
        }
    }

    @HandlesEvent(CHOOSE_EVENT_TYPE_ACTION)
    public Resolution chooseLabEventType() {
        List<String> reagentNames;
        int[] reagentFieldCounts;
        if (workflowStepDef != null) {
            reagentNames = workflowStepDef.getReagentTypes();
            reagentFieldCounts = new int[reagentNames.size()];
            Arrays.fill(reagentFieldCounts, 1);
        } else {
            reagentNames = Arrays.asList(manualTransferDetails.getReagentNames());
            reagentFieldCounts = manualTransferDetails.getReagentFieldCounts();
        }
        int reagentIndex = 0;
        for (String reagentName : reagentNames) {
            for (int fieldIndex = 0; fieldIndex < reagentFieldCounts[reagentIndex]; fieldIndex++) {
                ReagentType reagentType = new ReagentType();
                reagentType.setKitType(reagentName);
                stationEvents.get(0).getReagent().add(reagentType);
            }
            reagentIndex++;
        }

        int stationEventIndex = 0;
        for (StationEventType stationEvent : stationEvents) {
            if (manualTransferDetails.getSecondaryEvent() != null && stationEventIndex > 0) {
                stationEvent.setEventType(manualTransferDetails.getSecondaryEvent().getName());
            } else {
                stationEvent.setEventType(labEventType.getName());
            }
            switch (manualTransferDetails.getMessageType()) {
                case PLATE_EVENT:
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    PlateType plateType = new PlateType();
                    VesselTypeGeometry vesselTypeGeometry = manualTransferDetails.getTargetVesselTypeGeometry();
                    plateType.setPhysType(vesselTypeGeometry.getDisplayName());
                    plateEventType.setPlate(plateType);
                    if (vesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateEventType.setPositionMap(new PositionMapType());
                    }
                break;
                case PLATE_TRANSFER_EVENT:
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    PlateType sourcePlate = new PlateType();
                    VesselTypeGeometry sourceVesselTypeGeometry = manualTransferDetails.getSourceVesselTypeGeometry();
                    sourcePlate.setPhysType(sourceVesselTypeGeometry.getDisplayName());
                    plateTransferEventType.setSourcePlate(sourcePlate);
                    if (sourceVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateTransferEventType.setSourcePositionMap(new PositionMapType());
                    }

                    PlateType destinationPlateType = new PlateType();
                    VesselTypeGeometry targetVesselTypeGeometry = manualTransferDetails.getTargetVesselTypeGeometry();
                    destinationPlateType.setPhysType(targetVesselTypeGeometry.getDisplayName());
                    plateTransferEventType.setPlate(destinationPlateType);
                    if (targetVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateTransferEventType.setPositionMap(new PositionMapType());
                    }
                    break;
                case STATION_SETUP_EVENT:
                    StationSetupEvent stationEventType = (StationSetupEvent) stationEvent;
                    break;
                case PLATE_CHERRY_PICK_EVENT:
                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;
                    break;
                case RECEPTACLE_PLATE_TRANSFER_EVENT:
                    ReceptaclePlateTransferEvent receptaclePlateTransferEvent = (ReceptaclePlateTransferEvent) stationEvent;
                    break;
                case RECEPTACLE_EVENT:
                    ReceptacleEventType receptacleEventType = (ReceptacleEventType) stationEvent;
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setReceptacleType(workflowStepDef == null ?
                            manualTransferDetails.getTargetVesselTypeGeometry().getDisplayName() :
                            workflowStepDef.getTargetBarcodedTubeType().getDisplayName());
                    receptacleEventType.setReceptacle(receptacleType);
                    break;
                case RECEPTACLE_TRANSFER_EVENT:
                    ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                    ReceptacleType sourceReceptacle = new ReceptacleType();
                    sourceReceptacle.setReceptacleType(
                            manualTransferDetails.getSourceVesselTypeGeometry().getDisplayName());
                    receptacleTransferEventType.setSourceReceptacle(sourceReceptacle);

                    ReceptacleType destinationReceptacle = new ReceptacleType();
                    destinationReceptacle.setReceptacleType(
                            manualTransferDetails.getTargetVesselTypeGeometry().getDisplayName());
                    receptacleTransferEventType.setReceptacle(destinationReceptacle);
                    break;
                default:
                    throw new RuntimeException("Unknown labEventType " + manualTransferDetails.getMessageType());
            }
            stationEventIndex++;
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @Nullable
    public static WorkflowStepDef loadWorkflowStepDef(Date workflowEffectiveDate, WorkflowLoader workflowLoader,
            String workflowProcessName, String workflowStepName) {
        WorkflowStepDef workflowStepDef = null;
        if (workflowProcessName != null) {
            WorkflowConfig workflowConfig = workflowLoader.load();
            workflowStepDef = workflowConfig.getStep(workflowProcessName, workflowStepName,
                    workflowEffectiveDate);
            workflowStepDef.getReagentTypes();
        }
        return workflowStepDef;
    }

    @HandlesEvent(RACK_SCAN_EVENT)
    public Resolution rackScan() throws ScannerException {
        scan();
        StationEventType stationEventType = stationEvents.get(scanIndex);
        PositionMapType positionMapType = scanSource ? ((PlateTransferEventType) stationEventType).getSourcePositionMap() :
                ((PlateEventType) stationEventType).getPositionMap();
        for (Map.Entry<String, String> positionBarcodeEntry : rackScan.entrySet()) {
            ReceptacleType receptacleAtPosition = findReceptacleAtPosition(positionMapType, positionBarcodeEntry.getKey());
            if (receptacleAtPosition == null) {
                receptacleAtPosition = new ReceptacleType();
                positionMapType.getReceptacle().add(receptacleAtPosition);
            }
            receptacleAtPosition.setPosition(positionBarcodeEntry.getKey());
            receptacleAtPosition.setBarcode(positionBarcodeEntry.getValue());
        }

        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    /**
     * Called after the user has entered barcodes.  Fetches existing data, if any.
     * @return JSP
     */
    @HandlesEvent(FETCH_EXISTING_ACTION)
    public Resolution fetchExisting() {
        LabBatch labBatch = null;
        if (batchName != null) {
            labBatch = labBatchDao.findByName(batchName);
        }
        MessageCollection messageCollection = new MessageCollection();
        validateBarcodes(labBatch, messageCollection);
        addMessages(messageCollection);
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    private void validateBarcodes(LabBatch labBatch, MessageCollection messageCollection) {
        switch (manualTransferDetails.getMessageType()) {
            case PLATE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    loadPlateFromDb(plateEventType.getPlate(), plateEventType.getPositionMap(), true, labBatch,
                            messageCollection);
                }
                break;
            case PLATE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    Map<String, LabVessel> mapBarcodeToVessel = loadPlateFromDb(plateTransferEventType.getSourcePlate(),
                            plateTransferEventType.getSourcePositionMap(), true, labBatch, messageCollection);
                    LabEventType repeatedEvent = manualTransferDetails.getRepeatedEvent();
                    if (repeatedEvent != null) {
                        validateRepeatedEvent(plateTransferEventType, mapBarcodeToVessel, repeatedEvent,
                                manualTransferDetails.getRepeatedWorkflowQualifier(), messageCollection);
                    }
                    // todo jmt take required, empty from workflow
                    loadPlateFromDb(plateTransferEventType.getPlate(), plateTransferEventType.getPositionMap(), false,
                            labBatch, messageCollection);
                }
                break;
            case STATION_SETUP_EVENT:
                break;
            case PLATE_CHERRY_PICK_EVENT:
                break;
            case RECEPTACLE_PLATE_TRANSFER_EVENT:
                break;
            case RECEPTACLE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleEventType receptacleEventType = (ReceptacleEventType) stationEvent;
                    loadReceptacleFromDb(receptacleEventType.getReceptacle(), true, messageCollection);
                }
                break;
            case RECEPTACLE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                    loadReceptacleFromDb(receptacleTransferEventType.getSourceReceptacle(), true, messageCollection);
                    // todo jmt take required, empty from workflow
                    loadReceptacleFromDb(receptacleTransferEventType.getReceptacle(), false, messageCollection);
                }
                break;
        }
    }

    /**
     * Validate that the sources and destinations in a repeated event are the same as those in the event that it
     * repeats.
     * @param plateTransferEventType    second event
     * @param mapBarcodeToVessel        database entities
     * @param repeatedEvent             first event type
     * @param repeatedWorkflowQualifier first event qualifier
     * @param messageCollection         output, errors and infos
     */
    private static void validateRepeatedEvent(PlateTransferEventType plateTransferEventType,
            Map<String, LabVessel> mapBarcodeToVessel, LabEventType repeatedEvent,
            String repeatedWorkflowQualifier, MessageCollection messageCollection) {

        // For each vessel in source section, map to destination
        Map<String, String> mapSourceBarcodeToTargetBarcode = new HashMap<>();
        SBSSection sourceSection = SBSSection.getBySectionName(plateTransferEventType.getSourcePlate().getSection());
        SBSSection targetSection = SBSSection.getBySectionName(plateTransferEventType.getPlate().getSection());
        for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
            if (receptacleType.getBarcode() == null) {
                continue;
            }
            VesselPosition targetVesselPosition = targetSection.getWells().get(
                    sourceSection.getWells().indexOf(VesselPosition.getByName(receptacleType.getPosition())));
            for (ReceptacleType targetReceptacleType : plateTransferEventType.getPositionMap().getReceptacle()) {
                if (targetReceptacleType.getPosition().equals(targetVesselPosition.name())) {
                    mapSourceBarcodeToTargetBarcode.put(receptacleType.getBarcode(), targetReceptacleType.getBarcode());
                    break;
                }
            }
        }

        // Compare source and destination barcodes in this transfer to previous transfer
        int matches = 0;
        int errors = 0;
        for (String sourceBarcode : mapSourceBarcodeToTargetBarcode.keySet()) {
            LabVessel currentLabVessel = mapBarcodeToVessel.get(sourceBarcode);
            if (currentLabVessel == null) {
                // We expect loadPlateFromDb to have added an error for a missing source
                continue;
            }
            boolean found = false;
            for (LabEvent labEvent : currentLabVessel.getTransfersFrom()) {
                if (labEvent.getLabEventType() == repeatedEvent &&
                        (repeatedWorkflowQualifier == null ||
                                labEvent.getWorkflowQualifier().equals(repeatedWorkflowQualifier))) {
                    found = true;
                    SectionTransfer sectionTransfer = labEvent.getSectionTransfers().iterator().next();
                    VesselPosition sourceVesselPos = sectionTransfer.getSourceVesselContainer().getPositionOfVessel(
                            currentLabVessel);
                    int sourceIndex = sectionTransfer.getSourceSection().getWells().indexOf(sourceVesselPos);
                    LabVessel previousTargetVessel = sectionTransfer.getTargetVesselContainer().getVesselAtPosition(
                            sectionTransfer.getTargetSection().getWells().get(sourceIndex));
                    String currentTargetBarcode = mapSourceBarcodeToTargetBarcode.get(currentLabVessel.getLabel());
                    assert previousTargetVessel != null;
                    if (previousTargetVessel.getLabel().equals(currentTargetBarcode)) {
                        matches++;
                    } else {
                        messageCollection.addError("Expected " + previousTargetVessel.getLabel() + ", but found " +
                                currentTargetBarcode);
                        errors++;
                    }
                }
            }
            if (!found) {
                messageCollection.addError(currentLabVessel.getLabel() + " not found in previous message");
                errors++;
            }
        }
        if (matches == mapSourceBarcodeToTargetBarcode.size() && errors == 0) {
            messageCollection.addInfo("Transfer matches previous");
        }
    }

    private void loadReceptacleFromDb(ReceptacleType receptacleType, boolean required,
            MessageCollection messageCollection) {
        if (receptacleType != null) {
            String barcode = receptacleType.getBarcode();
            if (!StringUtils.isBlank(barcode)) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    if (required) {
                        messageCollection.addError(barcode + " is not in the database");
                    } else {
                        messageCollection.addInfo(barcode + " is not in the database");
                    }
                } else {
                    if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                        BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                        receptacleType.setReceptacleType(barcodedTube.getTubeType().getDisplayName());
                        if (barcodedTube.getVolume() != null) {
                            receptacleType.setVolume(barcodedTube.getVolume());
                        }
                        receptacleType.setConcentration(barcodedTube.getConcentration());
                        receptacleType.setReceptacleWeight(barcodedTube.getReceptacleWeight());
                        messageCollection.addInfo(barcode + " is in the database");
                    } else {
                        messageCollection.addError(barcode + " is not a tube");
                    }
                }
            }
        }
    }

    private Map<String, LabVessel> loadPlateFromDb(PlateType plateType, PositionMapType positionMapType,
            boolean required, LabBatch labBatch, MessageCollection messageCollection) {
        Map<String, LabVessel> returnMapBarcodeToVessel = new HashMap<>();
        if (plateType != null) {
            String barcode = plateType.getBarcode();
            if (!StringUtils.isBlank(barcode)) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    if (required) {
                        messageCollection.addError(barcode + " is not in the database");
                    } else {
                        messageCollection.addInfo(barcode + " is not in the database");
                    }
                } else {
                    messageCollection.addInfo(barcode + " is in the database");
                    returnMapBarcodeToVessel.put(labVessel.getLabel(), labVessel);
                }
            }
        }
        if (positionMapType != null) {
            List<String> barcodes = new ArrayList<>();
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                if (!StringUtils.isBlank(receptacleType.getBarcode())) {
                    barcodes.add(receptacleType.getBarcode());
                }
            }
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
            for (Map.Entry<String, LabVessel> stringLabVesselEntry : mapBarcodeToVessel.entrySet()) {
                LabVessel labVessel = stringLabVesselEntry.getValue();
                String barcode = stringLabVesselEntry.getKey();
                if (labVessel == null) {
                    if (required) {
                        messageCollection.addError(barcode + " is not in the database");
                    } else {
                        messageCollection.addInfo(barcode + " is not in the database");
                    }
                } else {
                    messageCollection.addInfo(barcode + " is in the database");
                    if (!labVessel.getNearestWorkflowLabBatches().contains(labBatch)) {
                        messageCollection.addInfo(barcode + " is not in batch " + labBatch.getBatchName());
                    }
                }
            }
            returnMapBarcodeToVessel.putAll(mapBarcodeToVessel);
        }
        return returnMapBarcodeToVessel;
    }

    @HandlesEvent(TRANSFER_ACTION)
    public Resolution transfer() {
        MessageCollection messageCollection = new MessageCollection();
        LabBatch labBatch = null;
        if (batchName != null) {
            labBatch = labBatchDao.findByName(batchName);
        }
        validateBarcodes(labBatch, messageCollection);
        // If there are only infos, ignore them
        if (!messageCollection.getErrors().isEmpty()) {
            addMessages(messageCollection);
        }

        for (ReagentType reagentType : stationEvents.get(0).getReagent()) {
            if (StringUtils.isBlank(reagentType.getKitType())) {
                addGlobalValidationError("Reagent type is required");
            }
            if (manualTransferDetails.getMapReagentNameToCount().get(reagentType.getKitType()) == 1) {
                if (StringUtils.isBlank(reagentType.getBarcode())) {
                    addGlobalValidationError("Reagent barcode is required");
                }
                if (manualTransferDetails.isExpirationDateIncluded() &&
                        reagentType.getExpiration() == null) {
                    addGlobalValidationError("Reagent expiration is required");
                }
            }
        }

        BettaLIMSMessage bettaLIMSMessage = null;
        if (getContext().getValidationErrors().isEmpty()) {
            // remove unused reagents
            Iterator<ReagentType> reagentIterator = stationEvents.get(0).getReagent().iterator();
            while (reagentIterator.hasNext()) {
                ReagentType reagentType = reagentIterator.next();
                if (StringUtils.isBlank(reagentType.getBarcode()) &&
                        manualTransferDetails.getMapReagentNameToCount().get(reagentType.getKitType()) > 1) {
                    reagentIterator.remove();
                }
            }
            bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
            Date start = new Date();
            Iterator<StationEventType> iterator = stationEvents.iterator();
            if (stationEvents.get(0).getStation() == null) {
                stationEvents.get(0).setStation(LabEvent.UI_EVENT_LOCATION);
            }

            int eventIndex = 0;
            while (iterator.hasNext()) {
                StationEventType stationEvent = iterator.next();
                if (workflowStepDef != null) {
                    stationEvent.setWorkflowQualifier(workflowStepDef.getWorkflowQualifier());
                }
                stationEvent.setOperator(getUserBean().getLoginUserName());
                stationEvent.setProgram(LabEvent.UI_PROGRAM_NAME);
                stationEvent.setDisambiguator((long) (eventIndex + 1));
                stationEvent.setStart(start);

                // Remove empty elements
                if (stationEvent instanceof PlateTransferEventType) {
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    cleanupPositionMap(plateTransferEventType.getSourcePositionMap(),
                            plateTransferEventType.getSourcePlate(),
                            manualTransferDetails.getSourceVesselTypeGeometry());
                    cleanupPositionMap(plateTransferEventType.getPositionMap(), plateTransferEventType.getPlate(),
                            manualTransferDetails.getTargetVesselTypeGeometry());
                    if (manualTransferDetails.getSecondaryEvent() != null && eventIndex > 0) {
                        // copy source from primary
                        PlateTransferEventType firstPlateTransferEventType =
                                (PlateTransferEventType) stationEvents.get(0);
                        plateTransferEventType.setSourcePlate(firstPlateTransferEventType.getSourcePlate());
                        plateTransferEventType.setSourcePositionMap(firstPlateTransferEventType.getSourcePositionMap());
                    }
                    bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
                } else if (stationEvent instanceof PlateEventType) {
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    // Remove events for which the user did not enter a barcode
                    if (manualTransferDetails.getNumEvents() > 1 && plateEventType.getPositionMap() == null &&
                            (plateEventType.getPlate() == null || plateEventType.getPlate().getBarcode() == null)) {
                        iterator.remove();
                        continue;
                    }
                    cleanupPositionMap(plateEventType.getPositionMap(), plateEventType.getPlate(),
                            manualTransferDetails.getTargetVesselTypeGeometry());
                    bettaLIMSMessage.getPlateEvent().add(plateEventType);
                } else if (stationEvent instanceof StationSetupEvent) {
                    bettaLIMSMessage.setStationSetupEvent((StationSetupEvent) stationEvent);
                } else if (stationEvent instanceof PlateCherryPickEvent) {
                    bettaLIMSMessage.getPlateCherryPickEvent().add((PlateCherryPickEvent) stationEvent);
                } else if (stationEvent instanceof ReceptaclePlateTransferEvent) {
                    bettaLIMSMessage.getReceptaclePlateTransferEvent().add((ReceptaclePlateTransferEvent) stationEvent);
                } else if (stationEvent instanceof ReceptacleTransferEventType) {
                    bettaLIMSMessage.getReceptacleTransferEvent().add((ReceptacleTransferEventType) stationEvent);
                } else if (stationEvent instanceof ReceptacleEventType) {
                    bettaLIMSMessage.getReceptacleEvent().add((ReceptacleEventType) stationEvent);
                } else {
                    throw new RuntimeException("Unknown StationEvent subclass " + stationEvent.getClass());
                }

                if (eventIndex > 0) {
                    stationEvent.getReagent().addAll(stationEvents.get(0).getReagent());
                    stationEvent.setStation(stationEvents.get(0).getStation());
                }
                eventIndex++;
            }
        }

        if (getContext().getValidationErrors().isEmpty()) {
            try {
                ObjectMarshaller<BettaLIMSMessage> bettaLIMSMessageObjectMarshaller =
                        new ObjectMarshaller<>(BettaLIMSMessage.class);
                bettaLimsMessageResource.storeAndProcess(bettaLIMSMessageObjectMarshaller.marshal(bettaLIMSMessage));
                addMessage("Transfer recorded successfully.");
            } catch (Exception e) {
                log.error("Failed to process message", e);
                addGlobalValidationError(e.getMessage());
            }
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    private void cleanupPositionMap(PositionMapType positionMapType, PlateType plate,
            VesselTypeGeometry vesselTypeGeometry) {
        if (positionMapType != null) {
            Iterator<ReceptacleType> iterator = positionMapType.getReceptacle().iterator();
            while (iterator.hasNext()) {
                ReceptacleType next = iterator.next();
                if (next.getBarcode() == null || next.getBarcode().isEmpty()) {
                    iterator.remove();
                }
            }
            String barcode;
            if (vesselTypeGeometry.isBarcoded()) {
                barcode = plate.getBarcode();
            } else {
                barcode = String.valueOf(System.currentTimeMillis()) + anonymousRackDisambiguator;
                anonymousRackDisambiguator++;
                plate.setBarcode(barcode);
            }
            positionMapType.setBarcode(barcode);
        }
    }

    public ReceptacleType findReceptacleAtPosition(PositionMapType positionMapType, String position) {
        // todo jmt create map in advance to improve performance?
        ReceptacleType receptacleTypeReturn = null;
        for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
            if (receptacleType.getPosition().equals(position)) {
                receptacleTypeReturn = receptacleType;
                break;
            }
        }
        return receptacleTypeReturn;
    }

    public List<StationEventType> getStationEvents() {
        return stationEvents;
    }

    public void setStationEvents(List<StationEventType> stationEvents) {
        this.stationEvents = stationEvents;
    }

    public List<LabEventType> getManualEventTypes() {
        List<LabEventType> manualLabEventTypes = new ArrayList<>();
        for (LabEventType eventType : LabEventType.values()) {
            if (eventType.getManualTransferDetails() != null) {
                manualLabEventTypes.add(eventType);
            }
        }
        return manualLabEventTypes;
    }

    public LabEventType getLabEventType() {
        return labEventType;
    }

    public String getWorkflowProcessName() {
        return workflowProcessName;
    }

    public void setWorkflowProcessName(String workflowProcessName) {
        this.workflowProcessName = workflowProcessName;
    }

    public String getWorkflowStepName() {
        return workflowStepName;
    }

    public void setWorkflowStepName(String workflowStepName) {
        this.workflowStepName = workflowStepName;
    }

    public Date getWorkflowEffectiveDate() {
        return workflowEffectiveDate;
    }

    public void setWorkflowEffectiveDate(Date workflowEffectiveDate) {
        this.workflowEffectiveDate = workflowEffectiveDate;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public WorkflowStepDef getWorkflowStepDef() {
        return workflowStepDef;
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    public void setScanIndex(Integer scanIndex) {
        this.scanIndex = scanIndex;
    }

    public void setScanSource(Boolean scanSource) {
        this.scanSource = scanSource;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
    }

    public LabEventType.ManualTransferDetails getManualTransferDetails() {
        return manualTransferDetails;
    }
}
