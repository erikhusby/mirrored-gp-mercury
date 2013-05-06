package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test messaging for BSP Dried Blood Spot Extraction
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class DriedBloodSpotDbFreeTest {
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MMddHHmmss");

    public void testEndToEnd() {
        // import batch and tubes
        String timestamp = timestampFormat.format(new Date());
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for(int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String barcode = "SM-FTA" + rackPosition + timestamp;
            tubeBeans.add(new TubeBean(barcode, null));
        }

        String batchId = "BP-2";
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        Map<MercurySample, MercurySample> mapSampleToSample = new LinkedHashMap<MercurySample, MercurySample>();
        LabBatch labBatch = labBatchResource.buildLabBatch(new LabBatchBean(batchId, "DBS", tubeBeans),
                mapBarcodeToTube, mapSampleToSample/*, null*/);

        DriedBloodSpotJaxbBuilder driedBloodSpotJaxbBuilder = new DriedBloodSpotJaxbBuilder(
                new ArrayList<String>(mapBarcodeToTube.keySet()), labBatch.getBatchName(), timestamp);
        driedBloodSpotJaxbBuilder.buildJaxb();
        DriedBloodSpotEntityBuilder driedBloodSpotEntityBuilder = new DriedBloodSpotEntityBuilder(
                driedBloodSpotJaxbBuilder, labBatch, mapBarcodeToTube);
        driedBloodSpotEntityBuilder.buildEntities();

        LabEventResource labEventResource = new LabEventResource();
        List<LabEventBean> labEventBeans = labEventResource.buildLabEventBeans(
                new ArrayList<LabEvent>(labBatch.getLabEvents()),
                new LabEventFactory.LabEventRefDataFetcher() {
                   @Override
                   public BspUser getOperator(String userId) {
                       BSPUserList testList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
                       return testList.getByUsername(userId);
                   }

                   @Override
                   public BspUser getOperator(Long bspUserId) {
                       BSPUserList testList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
                       return testList.getById(bspUserId);
                   }

                   @Override
                   public LabBatch getLabBatch(String labBatchName) {
                       return null;
                   }
               });
//        Assert.assertEquals("Wrong number of messages", 10, labEventBeans.size());
    }

    public static class DriedBloodSpotJaxbBuilder{
        private List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private List<ReceptaclePlateTransferEvent> samplePunchJaxbs = new ArrayList<ReceptaclePlateTransferEvent>();
        private PlateEventType incubationMixJaxb;
        private PlateEventType lysisBufferJaxb;
        private PlateEventType magneticResinJaxb;
        private PlateTransferEventType dbs1stPurificationJaxb;
        private PlateEventType dbsWashBufferJaxb;
        private PlateEventType dbsElutionBufferJaxb;
        private PlateTransferEventType dbsFinalTransferJaxb;
        private List<String> ftaPaperBarcodes;
        private String labBatchId;
        private String timestamp;

        public DriedBloodSpotJaxbBuilder(List<String> ftaPaperBarcodes, String labBatchId, String timestamp) {
            this.ftaPaperBarcodes = ftaPaperBarcodes;
            this.labBatchId = labBatchId;
            this.timestamp = timestamp;
        }

        public void buildJaxb() {
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();

            String incubationPlateBarcode = "DBSIncPlate" + timestamp;
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
            int paperNum = 1;
            for (String ftaPaperBarcode : ftaPaperBarcodes) {
                // DBSSamplePunch receptacle -> plate A01 etc.
                ReceptaclePlateTransferEvent samplePunchJaxb = bettaLimsMessageTestFactory.buildTubeToPlate(
                        "DBSSamplePunch", ftaPaperBarcode, incubationPlateBarcode, "96DeepWell",
                        bettaLimsMessageTestFactory.buildWellName(paperNum,
                                BettaLimsMessageTestFactory.WellNameType.LONG), "FTAPaper");
                paperNum++;
                samplePunchJaxb.setBatchId(labBatchId);
                samplePunchJaxbs.add(samplePunchJaxb);
                bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb);
                bettaLimsMessageTestFactory.advanceTime();
            }
            messageList.add(bettaLIMSMessage);

            // DBSIncubationMix plateEvent
            incubationMixJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSIncubationMix", incubationPlateBarcode);
            ReagentType reagentType = new ReagentType();
            reagentType.setKitType("Incubation Mix");
            reagentType.setBarcode("IncubationMix1234");
            incubationMixJaxb.getReagent().add(reagentType);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, incubationMixJaxb);

            // DBSLysisBuffer plateEvent
            lysisBufferJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSLysisBuffer", incubationPlateBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, lysisBufferJaxb);

            // DBSMagneticResin plateEVent
            magneticResinJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSMagneticResin", incubationPlateBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, magneticResinJaxb);

            String firstPurificationBarcode = "DBS1stPur" + timestamp;
            // DBS1stPurification plate -> plate
            dbs1stPurificationJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("DBS1stPurification", incubationPlateBarcode, firstPurificationBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, dbs1stPurificationJaxb);

            // DBSWashBuffer plateEvent
            dbsWashBufferJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSWashBuffer", firstPurificationBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, dbsWashBufferJaxb);

            // DBSElutionBuffer plateEvent
            dbsElutionBufferJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSElutionBuffer", firstPurificationBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, dbsElutionBufferJaxb);

            // DBSFinalTransfer plate -> rack
            List<String> finalTubeBarcodes = new ArrayList<String>();
            for(int i = 0; i < ftaPaperBarcodes.size(); i++) {
                finalTubeBarcodes.add("DBSFinal" + i + timestamp);
            }
            dbsFinalTransferJaxb = bettaLimsMessageTestFactory.buildPlateToRack("DBSFinalTransfer", firstPurificationBarcode,
                    "DBSFinal" + timestamp, finalTubeBarcodes);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, dbsFinalTransferJaxb);
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
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();
            driedBloodSpotJaxbBuilder.buildJaxb();
            LabEventFactory labEventFactory = new LabEventFactory();
            labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
                @Override
                public BspUser getOperator ( String userId ) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
                }

                @Override
                public BspUser getOperator ( Long bspUserId ) {
                    return new BSPUserList.QADudeUser("Test", BSPManagerFactoryStub.QA_DUDE_USER_ID);
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
                        bettaLimsMessageTestFactory.buildWellName(tubeNum,
                                BettaLimsMessageTestFactory.WellNameType.LONG));
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
            labEventFactory.buildFromBettaLimsPlateEventDbFree(driedBloodSpotJaxbBuilder.getDbsElutionBufferJaxb(), firstPurificationPlate);

            HashMap<String, TwoDBarcodedTube> mapBarcodeToTargetTubes = new HashMap<String, TwoDBarcodedTube>();
            labEventFactory.buildFromBettaLimsPlateToRackDbFree(driedBloodSpotJaxbBuilder.getDbsFinalTransferJaxb(),
                    firstPurificationPlate, mapBarcodeToTargetTubes, null);
            Set<SampleInstance> sampleInstances = mapBarcodeToTargetTubes.values().iterator().next().getSampleInstances();
            Assert.assertEquals(1, sampleInstances.size(), 1, "Wrong number of sample instances");
        }
    }
}
