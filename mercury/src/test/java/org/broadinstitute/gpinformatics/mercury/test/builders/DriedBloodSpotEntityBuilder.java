package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds an entity graph for Dried Blood Spot messages.
 */
public class DriedBloodSpotEntityBuilder {
    private DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder;
    private LabBatch labBatch;
    private Map<String, BarcodedTube> mapBarcodeToTube;

    public DriedBloodSpotEntityBuilder(DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder, LabBatch labBatch,
                                       Map<String, BarcodedTube> mapBarcodeToTube) {
        this.driedBloodSpotJaxbBuilder = driedBloodSpotJaxbBuilder;
        this.labBatch = labBatch;
        this.mapBarcodeToTube = mapBarcodeToTube;
    }

    public void buildEntities() {
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);
        driedBloodSpotJaxbBuilder.buildJaxb();
        LabEventFactory labEventFactory = new LabEventFactory(null, null);
        labEventFactory.setLabEventRefDataFetcher(new LabEventRefDataFetcher() {
            @Override
            public BspUser getOperator(String userId) {
                return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
            }

            @Override
            public BspUser getOperator(Long bspUserId) {
                return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
            }

            @Override
            public LabBatch getLabBatch(String labBatchName) {
                return labBatch;
            }
        });

        int tubeNum = 1;
        StaticPlate incubationPlate = null;
        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            LabEvent samplePunchEntity = labEventFactory.buildVesselToSectionDbFree(
                    driedBloodSpotJaxbBuilder.getSamplePunchJaxbs().get(tubeNum), barcodedTube, null,
                    bettaLimsMessageTestFactory.buildWellName(tubeNum,
                            BettaLimsMessageTestFactory.WellNameType.LONG));
            incubationPlate = (StaticPlate) samplePunchEntity.getTargetLabVessels().iterator().next();
            tubeNum++;
        }

        labEventFactory
                .buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getIncubationMixJaxb(), incubationPlate);
        labEventFactory
                .buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getLysisBufferJaxb(), incubationPlate);
        labEventFactory
                .buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getMagneticResinJaxb(), incubationPlate);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(incubationPlate.getLabel(), incubationPlate);
        LabEvent firstPurificationEntity = labEventFactory.buildFromBettaLims(
                driedBloodSpotJaxbBuilder.getDbs1stPurificationJaxb(), mapBarcodeToVessel);
        StaticPlate firstPurificationPlate =
                (StaticPlate) firstPurificationEntity.getTargetLabVessels().iterator().next();
        labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getDbsWashBufferJaxb(),
                firstPurificationPlate);
        labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getDbsElutionBufferJaxb(),
                firstPurificationPlate);

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(firstPurificationPlate.getLabel(), firstPurificationPlate);
        LabEvent dbsFinalTransferEntity = labEventFactory.buildFromBettaLims(
                driedBloodSpotJaxbBuilder.getDbsFinalTransferJaxb(), mapBarcodeToVessel);
        Set<SampleInstance> sampleInstances = dbsFinalTransferEntity.getTargetLabVessels().iterator().next().
                getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances();
        // todo jmt what to assert here?
//        Assert.assertEquals(sampleInstances.size(), 1, "Wrong number of sample instances");
    }
}
