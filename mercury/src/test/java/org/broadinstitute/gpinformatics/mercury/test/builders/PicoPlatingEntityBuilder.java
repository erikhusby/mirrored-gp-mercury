package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
* @author Scott Matthews
*         Date: 4/3/13
*         Time: 6:27 AM
*/
public class PicoPlatingEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory             labEventFactory;
    private final LabEventHandler             labEventHandler;

    private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
    private       TubeFormation                 normTubeFormation;
    private String rackBarcode;
    private       StaticPlate                   postNormPicoPlate;
    private String normalizationBarcode;
    private Map<String,TwoDBarcodedTube> normBarcodedTubeMap;

    public TubeFormation getNormTubeFormation() {
        return normTubeFormation;
    }

    public Map<String, TwoDBarcodedTube> getNormBarcodeToTubeMap() {

        return normBarcodedTubeMap;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public Map<String, TwoDBarcodedTube> getMapBarcodeToTube() {
        return mapBarcodeToTube;
    }

    public StaticPlate getPostNormPicoPlate() {
        return postNormPicoPlate;
    }

    public String getNormalizationBarcode() {
        return normalizationBarcode;
    }

    public PicoPlatingEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                    LabEventFactory labEventFactory,
                                    LabEventHandler labEventHandler, Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                                    String rackBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.rackBarcode = rackBarcode;
    }

    public PicoPlatingEntityBuilder invoke() {

        PicoPlatingJaxbBuilder jaxbBuilder =
                new PicoPlatingJaxbBuilder(rackBarcode, new ArrayList<String>(mapBarcodeToTube
                        .keySet()), "", bettaLimsMessageTestFactory);
        jaxbBuilder.invoke();


        LabEventTest.validateWorkflow(LabEventType.PICO_PLATING_BUCKET.getName(), mapBarcodeToTube.values());
        LabEvent picoPlatingBucket =
                labEventFactory.buildFromBettaLimsRackEventDbFree(jaxbBuilder.getPicoPlatingBucket(),
                        null, mapBarcodeToTube,null);
        labEventHandler.processEvent(picoPlatingBucket);
        TubeFormation initialTubeFormation = (TubeFormation) picoPlatingBucket.getInPlaceLabVessel();


        LabEventTest.validateWorkflow(LabEventType.PICO_PLATING_QC.getName(), mapBarcodeToTube.values());
        LabEvent picoQcEntity =
                labEventFactory.buildFromBettaLimsRackToPlateDbFree(jaxbBuilder
                        .getPicoPlatingQc(), mapBarcodeToTube, initialTubeFormation.getRacksOfTubes().iterator()
                        .next(), null);
        labEventHandler.processEvent(picoQcEntity);

        StaticPlate picoQcPlate = (StaticPlate) picoQcEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(picoQcPlate.getSampleInstances().size(), mapBarcodeToTube.values().size());


        LabEventTest.validateWorkflow(LabEventType.PICO_DILUTION_TRANSFER.getName(), picoQcPlate);
        LabEvent picoPlatingSetup1Entity = labEventFactory
                .buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder.getPicoPlatingSetup1(), picoQcPlate, null);
        labEventHandler.processEvent(picoPlatingSetup1Entity);
        StaticPlate picoPlatingSetup1Plate = (StaticPlate) picoPlatingSetup1Entity.getTargetLabVessels().iterator()
                .next();


        LabEventTest.validateWorkflow(LabEventType.PICO_BUFFER_ADDITION.getName(), picoPlatingSetup1Plate);
        LabEvent picoPlatingSetup2Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder
                .getPicoPlatingSetup2(), picoPlatingSetup1Plate, null);
        labEventHandler.processEvent(picoPlatingSetup2Entity);
        StaticPlate picoPlatingSetup2Plate = (StaticPlate) picoPlatingSetup2Entity.getTargetLabVessels().iterator()
                .next();


        LabEventTest.validateWorkflow(LabEventType.PICO_MICROFLUOR_TRANSFER.getName(), picoPlatingSetup2Plate);
        LabEvent picoPlatingSetup3Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder
                .getPicoPlatingSetup3(), picoPlatingSetup2Plate, null);
        labEventHandler.processEvent(picoPlatingSetup3Entity);
        StaticPlate picoPlatingSetup3Plate = (StaticPlate) picoPlatingSetup3Entity.getTargetLabVessels().iterator()
                .next();


        LabEventTest.validateWorkflow(LabEventType.PICO_STANDARDS_TRANSFER.getName(), picoPlatingSetup3Plate);
        LabEvent picoPlatingSetup4Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(jaxbBuilder
                .getPicoPlatingSetup4(), picoPlatingSetup3Plate, null);
        labEventHandler.processEvent(picoPlatingSetup4Entity);
        StaticPlate picoPlatingSetup4Plate = (StaticPlate) picoPlatingSetup4Entity.getTargetLabVessels().iterator()
                .next();


        Map<VesselPosition, TwoDBarcodedTube> normPositionToTube =
                new HashMap<VesselPosition, TwoDBarcodedTube>(initialTubeFormation.getContainerRole()
                        .getContainedVessels().size());

        SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");
        String timestamp = timestampFormat.format(new Date());
        for (TwoDBarcodedTube currTube : initialTubeFormation.getContainerRole().getContainedVessels()) {
            VesselPosition currTubePositon = initialTubeFormation.getContainerRole().getPositionOfVessel(currTube);

            TwoDBarcodedTube currNormTube = new TwoDBarcodedTube("Norm" + currTubePositon.name() + timestamp);

            normPositionToTube.put(currTubePositon, currNormTube);
        }
        TubeFormation normTubeFormation = new TubeFormation(normPositionToTube, initialTubeFormation.getRackType());


        LabEventTest.validateWorkflow(LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName(), picoPlatingSetup4Plate);
        LabEvent picoPlatingNormEntity =
                labEventFactory.buildFromBettaLimsRackToRackDbFree(jaxbBuilder
                        .getPicoPlatingNormalizaion(), initialTubeFormation, normTubeFormation);

        labEventHandler.processEvent(picoPlatingNormEntity);
        this.normTubeFormation = (TubeFormation) picoPlatingNormEntity.getTargetLabVessels().iterator().next();
        normalizationBarcode = jaxbBuilder.getPicoPlatingNormalizaionBarcode();

        normBarcodedTubeMap =
                new HashMap<String, TwoDBarcodedTube>(this.normTubeFormation.getContainerRole().getContainedVessels().size());

        for (TwoDBarcodedTube currTube : this.normTubeFormation.getContainerRole().getContainedVessels()) {
            normBarcodedTubeMap.put(currTube.getLabel(), currTube);
        }

        LabEventTest.validateWorkflow(LabEventType.PICO_PLATING_POST_NORM_PICO.getName(), this.normTubeFormation);
        LabEvent picoPlatingPostNormEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(jaxbBuilder
                .getPicoPlatingPostNormSetup(), this.normTubeFormation, null);
        labEventHandler.processEvent(picoPlatingPostNormEntity);

        postNormPicoPlate = (StaticPlate) picoPlatingPostNormEntity.getTargetLabVessels().iterator().next();


        return this;

    }
}
