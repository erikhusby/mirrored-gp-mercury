package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
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
        stationEvent = new PlateTransferEventType();
    }

    @HandlesEvent(TRANSFER_ACTION)
    public Resolution upload() {
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
