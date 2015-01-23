package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
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
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselTypeGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A Stripes Action Bean to record manual transfers.
 */
@UrlBinding("/labevent/manualtransfer.action")
public class ManualTransferActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(ManualTransferActionBean.class);

    public static final String MANUAL_TRANSFER_PAGE = "/labevent/manual_transfer.jsp";
    public static final String CHOOSE_EVENT_TYPE_ACTION = "chooseEventType";
    public static final String TRANSFER_ACTION = "transfer";

    private BettaLIMSMessage bettaLIMSMessage;
    private StationEventType stationEvent;
    private LabEventType labEventType;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    // machine names?
    // reagent types?
    // metadata types?
    // sections?
    // phys types?

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        String eventType = getContext().getRequest().getParameter("stationEvent.eventType");
        if (eventType != null) {
            labEventType = LabEventType.valueOf(eventType);
            // todo jmt move this to the enum?
            bettaLIMSMessage = new BettaLIMSMessage();
            switch (labEventType.getMessageType()) {
                case PLATE_EVENT:
                    PlateEventType plateEventType = new PlateEventType();
                    stationEvent = plateEventType;
                    bettaLIMSMessage.getPlateEvent().add(plateEventType);
                    break;
                case PLATE_TRANSFER_EVENT:
                    PlateTransferEventType plateTransferEventType = new PlateTransferEventType();
                    stationEvent = plateTransferEventType;
                    bettaLIMSMessage.getPlateTransferEvent().add(plateTransferEventType);
                    break;
                case STATION_SETUP_EVENT:
                    StationSetupEvent stationSetupEvent = new StationSetupEvent();
                    stationEvent = stationSetupEvent;
                    bettaLIMSMessage.setStationSetupEvent(stationSetupEvent);
                    break;
                case PLATE_CHERRY_PICK_EVENT:
                    PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
                    stationEvent = plateCherryPickEvent;
                    bettaLIMSMessage.getPlateCherryPickEvent().add(plateCherryPickEvent);
                    break;
                case RECEPTACLE_PLATE_TRANSFER_EVENT:
                    ReceptaclePlateTransferEvent receptaclePlateTransferEvent = new ReceptaclePlateTransferEvent();
                    stationEvent = receptaclePlateTransferEvent;
                    bettaLIMSMessage.getReceptaclePlateTransferEvent().add(receptaclePlateTransferEvent);
                    break;
                case RECEPTACLE_EVENT:
                    ReceptacleEventType receptacleEventType = new ReceptacleEventType();
                    stationEvent = receptacleEventType;
                    bettaLIMSMessage.getReceptacleEvent().add(receptacleEventType);
                    break;
                case RECEPTACLE_TRANSFER_EVENT:
                    ReceptacleTransferEventType receptacleTransferEventType = new ReceptacleTransferEventType();
                    stationEvent = receptacleTransferEventType;
                    bettaLIMSMessage.getReceptacleTransferEvent().add(receptacleTransferEventType);
                    break;
                default:
                    throw new RuntimeException("Unknown labEventType " + labEventType.getMessageType());
            }
        }
    }

    @HandlesEvent(CHOOSE_EVENT_TYPE_ACTION)
    public Resolution chooseLabEventType() {
        for (String reagentName : labEventType.getReagentNames()) {
            ReagentType reagentType = new ReagentType();
            reagentType.setKitType(reagentName);
            stationEvent.getReagent().add(reagentType);
        }

        switch (labEventType.getMessageType()) {
            case PLATE_EVENT:
                PlateEventType plateEventType = (PlateEventType) stationEvent;
                break;
            case PLATE_TRANSFER_EVENT:
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
                break;
            case RECEPTACLE_TRANSFER_EVENT:
                ReceptacleTransferEventType receptacleTransferEventType = (ReceptacleTransferEventType) stationEvent;
                ReceptacleType sourceReceptacle = new ReceptacleType();
                sourceReceptacle.setReceptacleType(labEventType.getSourceVesselTypeGeometry().getDisplayName());
                receptacleTransferEventType.setSourceReceptacle(sourceReceptacle);

                ReceptacleType destinationReceptacle = new ReceptacleType();
                destinationReceptacle.setReceptacleType(labEventType.getTargetVesselTypeGeometry().getDisplayName());
                receptacleTransferEventType.setReceptacle(destinationReceptacle);
                break;
            default:
                throw new RuntimeException("Unknown labEventType " + labEventType.getMessageType());
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    // choose transfer type?
    // choose vessel type?
    // AJAX fetch by barcode (source or destination), parameter for error if not exists
    // rack scan (source or destination)
    // add / remove reagents?
    // add / remove metadata?

    @HandlesEvent(TRANSFER_ACTION)
    public Resolution transfer() {
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        stationEvent.setEventType(labEventType.getName());
        stationEvent.setOperator(getUserBean().getLoginUserName());
        stationEvent.setProgram(LabEvent.UI_PROGRAM_NAME);
        stationEvent.setStation(LabEvent.UI_EVENT_LOCATION);
        stationEvent.setDisambiguator(1L);
        stationEvent.setStart(new Date());
        try {
            ObjectMarshaller<BettaLIMSMessage> bettaLIMSMessageObjectMarshaller =
                    new ObjectMarshaller<>(BettaLIMSMessage.class);
            bettaLimsMessageResource.storeAndProcess(bettaLIMSMessageObjectMarshaller.marshal(bettaLIMSMessage));
            addMessage("Transfer recorded successfully.");
        } catch (Exception e) {
            log.error("Failed to process message", e);
            addGlobalValidationError(e.getMessage());
        }
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }


    public StationEventType getStationEvent() {
        return stationEvent;
    }

    public void setStationEvent(StationEventType stationEvent) {
        this.stationEvent = stationEvent;
    }

    public List<LabEventType> getManualEventTypes() {
        ArrayList<LabEventType> manualLabEventTypes = new ArrayList<>();
        for (LabEventType eventType : LabEventType.values()) {
            if (eventType.getMessageType() != null) {
                manualLabEventTypes.add(eventType);
            }
        }
        return manualLabEventTypes;
    }
}
