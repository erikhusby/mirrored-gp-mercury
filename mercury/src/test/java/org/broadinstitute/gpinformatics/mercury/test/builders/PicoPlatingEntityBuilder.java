package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
* @author Scott Matthews
*         Date: 4/3/13
*         Time: 6:27 AM
*/
public class PicoPlatingEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory             labEventFactory;
    private final LabEventHandler             labEventHandler;

    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private       TubeFormation                 normTubeFormation;
    private String rackBarcode;
    private String normalizationBarcode;
    private Map<String,BarcodedTube> normBarcodedTubeMap;
    private String testPrefix;

    public TubeFormation getNormTubeFormation() {
        return normTubeFormation;
    }

    public Map<String, BarcodedTube> getNormBarcodeToTubeMap() {
        return normBarcodedTubeMap;
    }

    public String getNormalizationBarcode() {
        return normalizationBarcode;
    }

    public PicoPlatingEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                    LabEventFactory labEventFactory,
                                    LabEventHandler labEventHandler, Map<String, BarcodedTube> mapBarcodeToTube,
                                    String rackBarcode, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public PicoPlatingEntityBuilder invoke() {

        PicoPlatingJaxbBuilder jaxbBuilder = new PicoPlatingJaxbBuilder(rackBarcode,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, bettaLimsMessageTestFactory);
        jaxbBuilder.invoke();

        LabEventTest.validateWorkflow(LabEventType.PICO_PLATING_BUCKET.getName(), mapBarcodeToTube.values());
        LabEvent picoPlatingBucket =
                labEventFactory.buildFromBettaLimsRackEventDbFree(jaxbBuilder.getPicoPlatingBucket(),
                        null, mapBarcodeToTube,null);
        labEventHandler.processEvent(picoPlatingBucket);
        TubeFormation initialTubeFormation = (TubeFormation) picoPlatingBucket.getInPlaceLabVessel();

        LabEventTest.validateWorkflow(LabEventType.PICO_PLATING_QC.getName(), mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new LinkedHashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        LabEvent picoQcEntity = labEventFactory.buildFromBettaLims(jaxbBuilder.getPicoPlatingQc(), mapBarcodeToVessel);
        labEventHandler.processEvent(picoQcEntity);

        StaticPlate picoQcPlate = (StaticPlate) picoQcEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstanceV2> expectedSampleInstances = new HashSet<>();
        for (LabVessel labVessel : mapBarcodeToTube.values()) {
            expectedSampleInstances.addAll(labVessel.getSampleInstancesV2());
        }
        Assert.assertEquals(picoQcPlate.getSampleInstancesV2().size(), expectedSampleInstances.size());

        LabEventTest.validateWorkflow(LabEventType.PICO_DILUTION_TRANSFER.getName(), picoQcPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(picoQcPlate.getLabel(), picoQcPlate);
        LabEvent picoPlatingSetup1Entity = labEventFactory.buildFromBettaLims(jaxbBuilder.getPicoPlatingSetup1(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(picoPlatingSetup1Entity);
        StaticPlate picoPlatingSetup1Plate = (StaticPlate) picoPlatingSetup1Entity.getTargetLabVessels().iterator()
                .next();

        LabEventTest.validateWorkflow(LabEventType.PICO_BUFFER_ADDITION.getName(), picoPlatingSetup1Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(picoPlatingSetup1Plate.getLabel(), picoPlatingSetup1Plate);
        LabEvent picoPlatingSetup2Entity = labEventFactory.buildFromBettaLims(jaxbBuilder.getPicoPlatingSetup2(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(picoPlatingSetup2Entity);
        StaticPlate picoPlatingSetup2Plate = (StaticPlate) picoPlatingSetup2Entity.getTargetLabVessels().iterator()
                .next();

        LabEventTest.validateWorkflow(LabEventType.PICO_MICROFLUOR_TRANSFER.getName(), picoPlatingSetup2Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(picoPlatingSetup2Plate.getLabel(), picoPlatingSetup2Plate);
        LabEvent picoPlatingSetup3Entity = labEventFactory.buildFromBettaLims(jaxbBuilder.getPicoPlatingSetup3(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(picoPlatingSetup3Entity);
        StaticPlate picoPlatingSetup3Plate = (StaticPlate) picoPlatingSetup3Entity.getTargetLabVessels().iterator()
                .next();

        LabEventTest.validateWorkflow(LabEventType.PICO_STANDARDS_TRANSFER.getName(), picoPlatingSetup3Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(picoPlatingSetup3Plate.getLabel(), picoPlatingSetup3Plate);
        LabEvent picoPlatingSetup4Entity = labEventFactory.buildFromBettaLims(jaxbBuilder.getPicoPlatingSetup4(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(picoPlatingSetup4Entity);
        StaticPlate picoPlatingSetup4Plate = (StaticPlate) picoPlatingSetup4Entity.getTargetLabVessels().iterator()
                .next();

        LabEventTest.validateWorkflow(LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName(), picoPlatingSetup4Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(initialTubeFormation.getLabel(), initialTubeFormation);
        for (BarcodedTube barcodedTube : initialTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent picoPlatingNormEntity = labEventFactory.buildFromBettaLims(jaxbBuilder.getPicoPlatingNormalization(),
                mapBarcodeToVessel);

        labEventHandler.processEvent(picoPlatingNormEntity);
        normTubeFormation = (TubeFormation) picoPlatingNormEntity.getTargetLabVessels().iterator().next();
        normalizationBarcode = jaxbBuilder.getPicoPlatingNormalizationBarcode();

        normBarcodedTubeMap =
                new LinkedHashMap<>(normTubeFormation.getContainerRole().getContainedVessels().size());

        for (VesselPosition vesselPosition : normTubeFormation.getVesselGeometry().getVesselPositions()) {
            BarcodedTube vesselAtPosition =
                    normTubeFormation.getContainerRole().getVesselAtPosition(vesselPosition);
            if (vesselAtPosition != null) {
                normBarcodedTubeMap.put(vesselAtPosition.getLabel(), vesselAtPosition);
            }
        }

        LabEventTest.validateWorkflow(LabEventType.PICO_PLATING_POST_NORM_PICO.getName(), normTubeFormation);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(normTubeFormation.getLabel(), normTubeFormation);
        for (BarcodedTube barcodedTube : normTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent picoPlatingPostNormEntity = labEventFactory.buildFromBettaLims(
                jaxbBuilder.getPicoPlatingPostNormSetup(), mapBarcodeToVessel);
        labEventHandler.processEvent(picoPlatingPostNormEntity);

        return this;
    }
}
