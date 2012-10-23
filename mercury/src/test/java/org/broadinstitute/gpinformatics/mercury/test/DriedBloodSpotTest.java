package org.broadinstitute.gpinformatics.mercury.test;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.GenericLabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test messaging for BSP Dried Blood Spot Extraction
 */
public class DriedBloodSpotTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testEndToEnd() {
        // import batch and tubes
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for(int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String barcode = "SM-FTA" + rackPosition;
            tubeBeans.add(new TubeBean(barcode, null, null));
        }

        String batchId = "BP-2";
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        Map<MercurySample, MercurySample> mapSampleToSample = new LinkedHashMap<MercurySample, MercurySample>();
        LabBatch labBatch = labBatchResource.buildLabBatch(new LabBatchBean(batchId, "DBS", tubeBeans),
                mapBarcodeToTube, mapSampleToSample/*, null*/);

        DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder = new DriedBloodSpotJaxbBuilder(
                new ArrayList<String>(mapBarcodeToTube.keySet()), labBatch.getBatchName());
        driedBloodSpotJaxbBuilder.buildJaxb();
        DriedBloodSpotEntityBuilder driedBloodSpotEntityBuilder = new DriedBloodSpotEntityBuilder(
                driedBloodSpotJaxbBuilder, labBatch, mapBarcodeToTube);
        driedBloodSpotEntityBuilder.buildEntities();

        LabEventResource labEventResource = new LabEventResource();
        List<LabEventBean> labEventBeans = labEventResource.buildLabEventBeans(new ArrayList<GenericLabEvent>(labBatch.getLabEvents()));
        Assert.assertEquals("Wrong number of messages", 10, labEventBeans.size());
    }

    public static class DriedBloodSpotJaxbBuilder{
        private List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private List<ReceptaclePlateTransferEvent> samplePunchJaxbs = new ArrayList<ReceptaclePlateTransferEvent>();
        private PlateEventType incubationMixJaxb;
        private PlateEventType lysisBufferJaxb;
        private PlateEventType magneticResinJaxb;
        private PlateTransferEventType dbs1stPurificationJaxb;
        private PlateEventType dbsWashBufferJaxb;
        private PlateTransferEventType dbs2ndPurificationJaxb;
        private PlateEventType dbsElutionBufferJaxb;
        private PlateTransferEventType dbsFinalTransferJaxb;
        private ArrayList<String> ftaPaperBarcodes;
        private String labBatchId;

        public DriedBloodSpotJaxbBuilder(ArrayList<String> ftaPaperBarcodes, String labBatchId) {
            this.ftaPaperBarcodes = ftaPaperBarcodes;
            this.labBatchId = labBatchId;
        }

        public void buildJaxb() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

            String incubationPlateBarcode = "DBSIncPlate";
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            int paperNum = 1;
            for (String ftaPaperBarcode : ftaPaperBarcodes) {
                // DBSSamplePunch receptacle -> plate A01 etc.
                ReceptaclePlateTransferEvent samplePunchJaxb = bettaLimsMessageFactory.buildTubeToPlate(
                        "DBSSamplePunch", ftaPaperBarcode, incubationPlateBarcode, "96DeepWell",
                        bettaLimsMessageFactory.buildWellName(paperNum), "FTAPaper");
                samplePunchJaxb.setBatchId(labBatchId);
                samplePunchJaxbs.add(samplePunchJaxb);
                bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb);
                bettaLimsMessageFactory.advanceTime();
            }
            messageList.add(bettaLIMSMessage);

            // DBSIncubationMix plateEvent
            incubationMixJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSIncubationMix", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateEvent().add(incubationMixJaxb);
            messageList.add(bettaLIMSMessage2);
            bettaLimsMessageFactory.advanceTime();

            // DBSLysisBuffer plateEvent
            lysisBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSLysisBuffer", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage3 = new BettaLIMSMessage();
            bettaLIMSMessage3.getPlateEvent().add(lysisBufferJaxb);
            messageList.add(bettaLIMSMessage3);
            bettaLimsMessageFactory.advanceTime();

            // DBSMagneticResin plateEVent
            magneticResinJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSMagneticResin", incubationPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage4 = new BettaLIMSMessage();
            bettaLIMSMessage4.getPlateEvent().add(magneticResinJaxb);
            messageList.add(bettaLIMSMessage4);
            bettaLimsMessageFactory.advanceTime();

            String firstPurificationBarcode = "DBS1stPur";
            // DBS1stPurification plate -> plate
            dbs1stPurificationJaxb = bettaLimsMessageFactory.buildPlateToPlate("DBS1stPurification", incubationPlateBarcode, firstPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage5 = new BettaLIMSMessage();
            bettaLIMSMessage5.getPlateEvent().add(dbs1stPurificationJaxb);
            messageList.add(bettaLIMSMessage5);
            bettaLimsMessageFactory.advanceTime();

            // DBSWashBuffer plateEvent
            dbsWashBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSWashBuffer", firstPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage6 = new BettaLIMSMessage();
            bettaLIMSMessage6.getPlateEvent().add(dbsWashBufferJaxb);
            messageList.add(bettaLIMSMessage6);
            bettaLimsMessageFactory.advanceTime();

            String secondPurificationBarcode = "DBS2ndPur";
            // DBS2ndPurification plate -> plate
            dbs2ndPurificationJaxb = bettaLimsMessageFactory.buildPlateToPlate("DBS2ndPurification", firstPurificationBarcode, secondPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage7 = new BettaLIMSMessage();
            bettaLIMSMessage7.getPlateEvent().add(dbs2ndPurificationJaxb);
            messageList.add(bettaLIMSMessage7);
            bettaLimsMessageFactory.advanceTime();

            // DBSElutionBuffer plateEvent
            dbsElutionBufferJaxb = bettaLimsMessageFactory.buildPlateEvent("DBSElutionBuffer", secondPurificationBarcode);
            BettaLIMSMessage bettaLIMSMessage8 = new BettaLIMSMessage();
            bettaLIMSMessage8.getPlateEvent().add(dbsElutionBufferJaxb);
            messageList.add(bettaLIMSMessage8);
            bettaLimsMessageFactory.advanceTime();

            // DBSFinalTransfer plate -> rack
            List<String> finalTubeBarcodes = new ArrayList<String>();
            for(int i = 0; i < 3; i++) {
                finalTubeBarcodes.add("DBSFinal" + i);
            }
            dbsFinalTransferJaxb = bettaLimsMessageFactory.buildPlateToRack("DBSFinalTransfer", secondPurificationBarcode, "DBSFinal", finalTubeBarcodes);
            BettaLIMSMessage bettaLIMSMessage9 = new BettaLIMSMessage();
            bettaLIMSMessage9.getPlateEvent().add(dbsFinalTransferJaxb);
            messageList.add(bettaLIMSMessage9);
            bettaLimsMessageFactory.advanceTime();
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public List<ReceptaclePlateTransferEvent> getSamplePunchJaxbs() {
            return samplePunchJaxbs;
        }

        public PlateEventType getIncubationMixJaxb() {
            return incubationMixJaxb;
        }

        public PlateEventType getLysisBufferJaxb() {
            return lysisBufferJaxb;
        }

        public PlateEventType getMagneticResinJaxb() {
            return magneticResinJaxb;
        }

        public PlateTransferEventType getDbs1stPurificationJaxb() {
            return dbs1stPurificationJaxb;
        }

        public PlateEventType getDbsWashBufferJaxb() {
            return dbsWashBufferJaxb;
        }

        public PlateTransferEventType getDbs2ndPurificationJaxb() {
            return dbs2ndPurificationJaxb;
        }

        public PlateEventType getDbsElutionBufferJaxb() {
            return dbsElutionBufferJaxb;
        }

        public PlateTransferEventType getDbsFinalTransferJaxb() {
            return dbsFinalTransferJaxb;
        }
    }

    public static class DriedBloodSpotEntityBuilder {
        private DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder;
        private LabBatch labBatch;
        private Map<String, TwoDBarcodedTube> mapBarcodeToTube;

        public DriedBloodSpotEntityBuilder(DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder, LabBatch labBatch,
                Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
            this.driedBloodSpotJaxbBuilder = driedBloodSpotJaxbBuilder;
            this.labBatch = labBatch;
            this.mapBarcodeToTube = mapBarcodeToTube;
        }

        public void buildEntities() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
            driedBloodSpotJaxbBuilder.buildJaxb();
            LabEventFactory labEventFactory = new LabEventFactory();
            labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
                @Override
                public Person getOperator(String userId) {
                    return new Person(userId);
                }

                @Override
                public LabBatch getLabBatch(String labBatchName) {
                    return labBatch;
                }
            });

            int tubeNum = 1;
            StaticPlate incubationPlate = null;
            for (TwoDBarcodedTube twoDBarcodedTube : mapBarcodeToTube.values()) {
                LabEvent samplePunchEntity = labEventFactory.buildVesselToSectionDbFree(
                        driedBloodSpotJaxbBuilder.getSamplePunchJaxbs().get(tubeNum), twoDBarcodedTube, null,
                        bettaLimsMessageFactory.buildWellName(tubeNum));
                incubationPlate = (StaticPlate) samplePunchEntity.getTargetLabVessels().iterator().next();
                tubeNum++;
            }

            labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getIncubationMixJaxb(), incubationPlate);
            labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getLysisBufferJaxb(), incubationPlate);
            labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getMagneticResinJaxb(), incubationPlate);

            LabEvent firstPurificationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    driedBloodSpotJaxbBuilder.getDbs1stPurificationJaxb(), incubationPlate, null);
            StaticPlate firstPurificationPlate = (StaticPlate) firstPurificationEntity.getTargetLabVessels().iterator().next();
            labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getDbsWashBufferJaxb(), firstPurificationPlate);

            LabEvent secondPurificationEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    driedBloodSpotJaxbBuilder.getDbs2ndPurificationJaxb(), firstPurificationPlate, null);
            StaticPlate secondPurificationPlate = (StaticPlate) secondPurificationEntity.getTargetLabVessels().iterator().next();
            labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getDbsElutionBufferJaxb(), secondPurificationPlate);

            HashMap<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = new HashMap<String, TwoDBarcodedTube>();
            labEventFactory.buildFromBettaLimsPlateToRackDbFree(driedBloodSpotJaxbBuilder.getDbsFinalTransferJaxb(),
                    secondPurificationPlate, mapBarcodeToTargetTubes);
//            Set<SampleInstance> sampleInstances = mapBarcodeToTargetTubes.values().iterator().next().getSampleInstances();
//            Assert.assertEquals("Wrong number of sample instances", 1, sampleInstances.size());
        }
    }
}
