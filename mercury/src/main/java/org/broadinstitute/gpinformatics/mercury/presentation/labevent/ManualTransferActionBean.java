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
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A Stripes Action Bean to record manual transfers.
 */
@UrlBinding("/labevent/manualtransfer.action")
public class ManualTransferActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(ManualTransferActionBean.class);

    public static final String MANUAL_TRANSFER_PAGE = "/labevent/manual_transfer.jsp";
    public static final String CHOOSE_EVENT_TYPE_ACTION = "chooseEventType";
    public static final String TRANSFER_ACTION = "transfer";
    public static final String FETCH_EXISTING_ACTION = "fetchExisting";

    /** Parameter from batch workflow page. */
    private String workflowProcessName;
    /** Parameter from batch workflow page. */
    private String workflowStepName;
    /** Parameter from batch workflow page. */
    private Date workflowEffectiveDate;
    /** Parameter from batch workflow page. */
    private String batchName;
    /** Loaded based on parameters. */
    private WorkflowStepDef workflowStepDef;

    /** POSTed from the form. */
    private List<StationEventType> stationEvents = new ArrayList<>();
    /** Set in the init method, from a POSTed parameter. */
    private LabEventType labEventType;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private WorkflowLoader workflowLoader;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    // todo jmt allow multiple events per message.  Specified by: user; batch size; event type.
    // Allow geometry to lay out plate or tube event barcodes on page (not transfers, they have to have their own geometry).
    // Populate metadata
    // This is all short term, until can generalize TubeFormation to support plates.

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        String eventType = getContext().getRequest().getParameter("stationEvents[0].eventType");
        if (eventType != null) {
            labEventType = LabEventType.valueOf(eventType);
            // todo jmt move this to the enum?
            switch (labEventType.getMessageType()) {
                case PLATE_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        PlateEventType plateEventType = new PlateEventType();
                        stationEvents.add(plateEventType);
                    }
                    break;
                case PLATE_TRANSFER_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        PlateTransferEventType plateTransferEventType = new PlateTransferEventType();
                        stationEvents.add(plateTransferEventType);
                    }
                    break;
                case STATION_SETUP_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        StationSetupEvent stationSetupEvent = new StationSetupEvent();
                        stationEvents.add(stationSetupEvent);
                    }
                    break;
                case PLATE_CHERRY_PICK_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
                        stationEvents.add(plateCherryPickEvent);
                    }
                    break;
                case RECEPTACLE_PLATE_TRANSFER_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        ReceptaclePlateTransferEvent receptaclePlateTransferEvent = new ReceptaclePlateTransferEvent();
                        stationEvents.add(receptaclePlateTransferEvent);
                    }
                    break;
                case RECEPTACLE_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        ReceptacleEventType receptacleEventType = new ReceptacleEventType();
                        stationEvents.add(receptacleEventType);
                    }
                    break;
                case RECEPTACLE_TRANSFER_EVENT:
                    for (int i = 0; i < labEventType.getNumEvents(); i++) {
                        ReceptacleTransferEventType receptacleTransferEventType = new ReceptacleTransferEventType();
                        stationEvents.add(receptacleTransferEventType);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown labEventType " + labEventType.getMessageType());
            }
        }
    }

    @HandlesEvent(CHOOSE_EVENT_TYPE_ACTION)
    public Resolution chooseLabEventType() {
        loadWorkflowStepDef();
        List<String> reagentNames;
        if (workflowStepDef != null) {
            reagentNames = workflowStepDef.getReagentTypes();
        } else {
            reagentNames = Arrays.asList(labEventType.getReagentNames());
        }
        for (String reagentName : reagentNames) {
            ReagentType reagentType = new ReagentType();
            reagentType.setKitType(reagentName);
            stationEvents.get(0).getReagent().add(reagentType);
        }

        switch (labEventType.getMessageType()) {
            case PLATE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    PlateType destinationPlateType = new PlateType();
                    VesselTypeGeometry targetVesselTypeGeometry = labEventType.getTargetVesselTypeGeometry();
                    destinationPlateType.setPhysType(targetVesselTypeGeometry.getDisplayName());
                    plateEventType.setPlate(destinationPlateType);
                    if (targetVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateEventType.setPositionMap(new PositionMapType());
                    }
                }
                break;
            case PLATE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    PlateType sourcePlate = new PlateType();
                    VesselTypeGeometry sourceVesselTypeGeometry = labEventType.getSourceVesselTypeGeometry();
                    sourcePlate.setPhysType(sourceVesselTypeGeometry.getDisplayName());
                    plateTransferEventType.setSourcePlate(sourcePlate);
                    if (sourceVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateTransferEventType.setSourcePositionMap(new PositionMapType());
                    }

                    PlateType destinationPlateType = new PlateType();
                    VesselTypeGeometry targetVesselTypeGeometry = labEventType.getTargetVesselTypeGeometry();
                    destinationPlateType.setPhysType(targetVesselTypeGeometry.getDisplayName());
                    plateTransferEventType.setPlate(destinationPlateType);
                    if (targetVesselTypeGeometry instanceof RackOfTubes.RackType) {
                        plateTransferEventType.setPositionMap(new PositionMapType());
                    }
                }
                break;
            case STATION_SETUP_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    StationSetupEvent stationEventType = (StationSetupEvent) stationEvent;
                }
                break;
            case PLATE_CHERRY_PICK_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateCherryPickEvent plateCherryPickEvent = (PlateCherryPickEvent) stationEvent;
                }
                break;
            case RECEPTACLE_PLATE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptaclePlateTransferEvent receptaclePlateTransferEvent = (ReceptaclePlateTransferEvent) stationEvent;
                }
                break;
            case RECEPTACLE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleEventType receptacleEventType = (ReceptacleEventType) stationEvent;
                    ReceptacleType receptacleType = new ReceptacleType();
                    receptacleType.setReceptacleType(workflowStepDef ==  null ?
                            labEventType.getTargetVesselTypeGeometry().getDisplayName() :
                            workflowStepDef.getTargetBarcodedTubeType().getDisplayName());
                    receptacleEventType.setReceptacle(receptacleType);
                }
                break;
            case RECEPTACLE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                    ReceptacleType sourceReceptacle = new ReceptacleType();
                    sourceReceptacle.setReceptacleType(labEventType.getSourceVesselTypeGeometry().getDisplayName());
                    receptacleTransferEventType.setSourceReceptacle(sourceReceptacle);

                    ReceptacleType destinationReceptacle = new ReceptacleType();
                    destinationReceptacle.setReceptacleType(labEventType.getTargetVesselTypeGeometry().getDisplayName());
                    receptacleTransferEventType.setReceptacle(destinationReceptacle);
                }
                break;
            default:
                throw new RuntimeException("Unknown labEventType " + labEventType.getMessageType());
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @Nullable
    private WorkflowStepDef loadWorkflowStepDef() {
        workflowStepDef = null;
        if (workflowProcessName != null) {
            WorkflowConfig workflowConfig = workflowLoader.load();
            workflowStepDef = workflowConfig.getStep(workflowProcessName, workflowStepName,
                    workflowEffectiveDate);
            workflowStepDef.getReagentTypes();
        }
        return workflowStepDef;
    }

    /**
     * Called after the user has entered barcodes.  Fetches existing data, if any.
     * @return JSP
     */
    @HandlesEvent(FETCH_EXISTING_ACTION)
    public Resolution fetchExisting() {
        switch (labEventType.getMessageType()) {
            case PLATE_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateEventType plateEventType = (PlateEventType) stationEvent;
                    loadPlateFromDb(plateEventType.getPlate(), plateEventType.getPositionMap(), true);
                }
                break;
            case PLATE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;
                    loadPlateFromDb(plateTransferEventType.getSourcePlate(), plateTransferEventType.getSourcePositionMap(), true);
                    loadPlateFromDb(plateTransferEventType.getPlate(), plateTransferEventType.getPositionMap(), false);
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
                    loadReceptacleFromDb(receptacleEventType.getReceptacle(), true);
                }
                break;
            case RECEPTACLE_TRANSFER_EVENT:
                for (StationEventType stationEvent : stationEvents) {
                    ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                    loadReceptacleFromDb(receptacleTransferEventType.getSourceReceptacle(), true);
                    loadReceptacleFromDb(receptacleTransferEventType.getReceptacle(), false);
                }
                break;
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    private void loadReceptacleFromDb(ReceptacleType receptacleType, boolean required) {
        if (receptacleType != null) {
            String barcode = receptacleType.getBarcode();
            if (!StringUtils.isBlank(barcode)) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    if (required) {
                        addGlobalValidationError("{2} is not in the database", barcode);
                    } else {
                        addMessage("{0} is not in the database", barcode);
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
                        addMessage("{0} is in the database", barcode);
                    } else {
                        addGlobalValidationError(barcode + " is not a tube");
                    }
                }
            }
        }
    }

    private void loadPlateFromDb(PlateType plateType, PositionMapType positionMapType, boolean required) {
        if (plateType != null) {
            String barcode = plateType.getBarcode();
            if (!StringUtils.isBlank(barcode)) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    if (required) {
                        addGlobalValidationError("{2} is not in the database", barcode);
                    } else {
                        addMessage("{0} is not in the database", barcode);
                    }
                } else {
                    addMessage("{0} is in the database", barcode);
                }
            }
        }
        if (positionMapType != null) {
            List<String> barcodes = new ArrayList<>();
            for (ReceptacleType receptacleType : positionMapType.getReceptacle()) {
                barcodes.add(receptacleType.getBarcode());
            }
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
            for (Map.Entry<String, LabVessel> stringLabVesselEntry : mapBarcodeToVessel.entrySet()) {
                if (stringLabVesselEntry.getValue() == null) {
                    addMessage("{0} is not in the database", stringLabVesselEntry.getKey());
                } else {
                    addMessage("{0} is in the database", stringLabVesselEntry.getKey());
                }
            }
        }
    }

    @HandlesEvent(TRANSFER_ACTION)
    public Resolution transfer() {
        // todo jmt handle unique constraint violation, increment disambiguator?
        loadWorkflowStepDef();

        for (ReagentType reagentType : stationEvents.get(0).getReagent()) {
            if (StringUtils.isBlank(reagentType.getKitType())) {
                addGlobalValidationError("Reagent type is required");
            }
            if (StringUtils.isBlank(reagentType.getBarcode())) {
                addGlobalValidationError("Reagent barcode is required");
            }
            if (reagentType.getExpiration() == null) {
                addGlobalValidationError("Reagent expiration is required");
            }
        }

        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        Date start = new Date();
        int eventIndex = 0;
        Iterator<StationEventType> iterator = stationEvents.iterator();
        if (stationEvents.get(0).getStation() == null) {
            stationEvents.get(0).setStation(LabEvent.UI_EVENT_LOCATION);
        }
        while (iterator.hasNext()) {
            StationEventType stationEvent = iterator.next();
            stationEvent.setEventType(labEventType.getName());
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
                cleanupPositionMap(plateTransferEventType.getSourcePositionMap(), plateTransferEventType.getSourcePlate(),
                        labEventType.getSourceVesselTypeGeometry());
                cleanupPositionMap(plateTransferEventType.getPositionMap(), plateTransferEventType.getPlate(),
                        labEventType.getTargetVesselTypeGeometry());
                bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
            } else if (stationEvent instanceof PlateEventType) {
                PlateEventType plateEventType = (PlateEventType) stationEvent;
                // Remove events for which the user did not enter a barcode
                if (labEventType.getNumEvents() > 1 && plateEventType.getPositionMap() == null &&
                        (plateEventType.getPlate() == null || plateEventType.getPlate().getBarcode() == null)) {
                    iterator.remove();
                    continue;
                }
                cleanupPositionMap(plateEventType.getPositionMap(), plateEventType.getPlate(),
                        labEventType.getTargetVesselTypeGeometry());
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
                barcode = String.valueOf(System.currentTimeMillis());
                plate.setBarcode(barcode);
            }
            positionMapType.setBarcode(barcode);
        }
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
            if (eventType.getMessageType() != null) {
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
}
