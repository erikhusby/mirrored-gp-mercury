package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationSetupEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.math.BigDecimal;

/**
 * A Stripes Action Bean to record manual transfers.
 */
@UrlBinding("/labevent/manualtransfer.action")
public class ManualTransferActionBean extends CoreActionBean {

    public static final String MANUAL_TRANSFER_PAGE = "/labevent/manual_transfer.jsp";
    public static final String TRANSFER_ACTION = "transfer";

    private StationEventType stationEvent;

    // machine names
    // reagent types?
    // metadata types?

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        PlateTransferEventType plateTransferEventType = new PlateTransferEventType();
        plateTransferEventType.setEventType(LabEventType.SHEARING_TRANSFER.getName());
        plateTransferEventType.setStation("SPIDERMAN");
//        plateTransferEventType.setStart();
//        plateTransferEventType.setDisambiguator();
//        plateTransferEventType.setOperator();

        PlateType sourcePlate = new PlateType();
        String rack1Barcode = "RACK1";
        sourcePlate.setBarcode(rack1Barcode);
        sourcePlate.setPhysType(LabEventFactory.PHYS_TYPE_TUBE_RACK);
        sourcePlate.setSection(LabEventFactory.SECTION_ALL_96);
        plateTransferEventType.setSourcePlate(sourcePlate);

        PositionMapType sourcePositionMap = new PositionMapType();
        sourcePositionMap.setBarcode(rack1Barcode);
        ReceptacleType receptacleType = new ReceptacleType();
        receptacleType.setBarcode("TUBE1");
        receptacleType.setPosition("A01");
        receptacleType.setReceptacleType("tube");
        receptacleType.setVolume(new BigDecimal("20.00"));
        sourcePositionMap.getReceptacle().add(receptacleType);
        plateTransferEventType.setSourcePositionMap(sourcePositionMap);

        PlateType destinationPlateType = new PlateType();
        destinationPlateType.setBarcode("PLATE1");
        destinationPlateType.setPhysType(LabEventFactory.PHYS_TYPE_EPPENDORF_96);
        destinationPlateType.setSection(LabEventFactory.SECTION_ALL_96);
        plateTransferEventType.setPlate(destinationPlateType);

        PositionMapType destinationPositionMapType = new PositionMapType();
        plateTransferEventType.setPositionMap(destinationPositionMapType);

        stationEvent = plateTransferEventType;

        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        String eventType = getContext().getRequest().getParameter("stationEvent.eventType");
        if (eventType == null) {
            throw new RuntimeException("Failed to find eventType");
        }
        LabEventType labEventType = LabEventType.getByName(eventType);
        // todo jmt move this to a factory
        switch (labEventType.getMessageType()) {
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
            default:
                throw new RuntimeException("Unknown labEventType " + labEventType.getMessageType());
        }
        /* How to determine geometry of  */
//        labEventType.getSourceVesselType;
//        labEventType.getTargetVesselType;
    }

    // choose lab event type
    // choose transfer type?
    // choose vessel type?
    // fetch by barcode (source or destination)
    // add / remove reagents?
    // add / remove metadata?

    @HandlesEvent(TRANSFER_ACTION)
    public Resolution transfer() {
        stationEvent.getOperator();
        return new ForwardResolution(MANUAL_TRANSFER_PAGE);
    }


    public StationEventType getStationEvent() {
        return stationEvent;
    }

    public void setStationEvent(StationEventType stationEvent) {
        this.stationEvent = stationEvent;
    }
}
