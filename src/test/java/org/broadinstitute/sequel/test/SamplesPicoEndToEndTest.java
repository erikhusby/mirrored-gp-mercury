package org.broadinstitute.sequel.test;

import junit.framework.Assert;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.sequel.boundary.vessel.LabBatchBean;
import org.broadinstitute.sequel.boundary.vessel.LabBatchResource;
import org.broadinstitute.sequel.boundary.vessel.TubeBean;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test BSP Pico messaging
 */
public class SamplesPicoEndToEndTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testAll() {
        // import batch, workflow and tubes
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for(int rackPosition = 1; rackPosition <= 8; rackPosition++) {
            String barcode = "R" + rackPosition;
            String bspStock = "SM-" + rackPosition;
            tubeBeans.add(new TubeBean(barcode, bspStock));
        }
        String batchId = "BP-1";
        BasicProjectPlan projectPlan = labBatchResource.buildProjectPlan(new LabBatchBean(batchId, "HybSel", tubeBeans),
                new HashMap<String, TwoDBarcodedTube>(), new HashMap<String, BSPStartingSample>());

        // validate workflow?
        // messaging
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (Starter starter : projectPlan.getStarters()) {
            LabVessel aliquotForStarter = projectPlan.getAliquotForStarter(starter);
            mapBarcodeToTube.put(aliquotForStarter.getLabel(), (TwoDBarcodedTube) aliquotForStarter);
        }

        SamplesPicoMessageBuilder samplesPicoMessageBuilder = new SamplesPicoMessageBuilder(mapBarcodeToTube,
                projectPlan.getProject().getJiraTicket().getLabBatch());
        samplesPicoMessageBuilder.buildEntities();
        // event web service, by batch
        // transfer visualizer?
        // datamart?
    }

    /**
     * Build the messages used in Samples (BSP) Pico batches
     */
    @SuppressWarnings("FeatureEnvy")
    public static class SamplesPicoMessageBuilder {
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
        private final LabBatch labBatch;

        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private PlateTransferEventType picoDilutionTransferJaxbA1;
        private PlateTransferEventType picoDilutionTransferJaxbA2;
        private PlateTransferEventType picoDilutionTransferJaxbB1;
        private PlateTransferEventType picoMicroflourTransferJaxb;
        private PlateTransferEventType picoStandardsTransferCol2Jaxb;
        private PlateTransferEventType picoStandardsTransferCol4Jaxb;
        private PlateTransferEventType picoStandardsTransferCol6Jaxb;
        private PlateTransferEventType picoStandardsTransferCol8Jaxb;
        private PlateTransferEventType picoStandardsTransferCol10Jaxb;
        private PlateTransferEventType picoStandardsTransferCol12Jaxb;

        SamplesPicoMessageBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube, LabBatch labBatch) {
            this.mapBarcodeToTube = mapBarcodeToTube;
            this.labBatch = labBatch;
        }

        /**
         * Build JAXB messages.  These messages can be sent to BettalimsMessageResource, or used to build entity
         * graphs.
         */
        void buildJaxb() {
            BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

            // 3 x  PicoDilutionTransfer
            String picoDilutionPlateBarcode = "PicoDilutionPlate";
            picoDilutionTransferJaxbA1 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA1.getPlate().setSection(SBSSection.A1.getSectionName());
            picoDilutionTransferJaxbA1.setBatchId(labBatch.getBatchName());

            picoDilutionTransferJaxbA2 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA2.getPlate().setSection(SBSSection.A2.getSectionName());
            picoDilutionTransferJaxbA2.setBatchId(labBatch.getBatchName());

            picoDilutionTransferJaxbB1 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbB1.getPlate().setSection(SBSSection.B1.getSectionName());
            picoDilutionTransferJaxbB1.setBatchId(labBatch.getBatchName());

            BettaLIMSMessage dilutionTransferMessage = new BettaLIMSMessage();
            dilutionTransferMessage.getPlateTransferEvent().add(picoDilutionTransferJaxbA1);
            dilutionTransferMessage.getPlateTransferEvent().add(picoDilutionTransferJaxbA2);
            dilutionTransferMessage.getPlateTransferEvent().add(picoDilutionTransferJaxbB1);
            messageList.add(dilutionTransferMessage);

/*
            // PicoBufferTransfer
            PlateTransferEventType picoBufferTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoBufferTransfer", "PicoBufferPlate", picoDilutionPlateBarcode);
            BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
            bettaLIMSMessage2.getPlateTransferEvent().add(picoBufferTransferJaxb);
            messageList.add(bettaLIMSMessage2);

*/
            // PicoMicroflourTransfer
            String picoMicroflourPlateBarcode = "PicoMicroflourPlate";
            picoMicroflourTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoMicroflourTransfer", picoDilutionPlateBarcode, picoMicroflourPlateBarcode);
            picoMicroflourTransferJaxb.getSourcePlate().setSection(SBSSection.ALL384.getSectionName());
            picoMicroflourTransferJaxb.getPlate().setSection(SBSSection.ALL384.getSectionName());
            picoMicroflourTransferJaxb.setBatchId(labBatch.getBatchName());

            BettaLIMSMessage microflourTransferMessage = new BettaLIMSMessage();
            microflourTransferMessage.getPlateTransferEvent().add(picoMicroflourTransferJaxb);
            messageList.add(microflourTransferMessage);

            // 6 x PicoStandardsTransfer
            picoStandardsTransferCol2Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferCol2Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol2Jaxb.getPlate().setSection(SBSSection.P384_COL2_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol2Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol4Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferCol4Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol4Jaxb.getPlate().setSection(SBSSection.P384_COL4_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol4Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol6Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferCol6Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol6Jaxb.getPlate().setSection(SBSSection.P384_COL6_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol6Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol8Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferCol8Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol8Jaxb.getPlate().setSection(SBSSection.P384_COL8_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol8Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol10Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferCol10Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol10Jaxb.getPlate().setSection(SBSSection.P384_COL10_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol10Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol12Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicroflourPlateBarcode);
            picoStandardsTransferCol12Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol12Jaxb.getPlate().setSection(SBSSection.P384_COL12_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol12Jaxb.setBatchId(labBatch.getBatchName());

            BettaLIMSMessage standardsTransferMessage = new BettaLIMSMessage();
            standardsTransferMessage.getPlateTransferEvent().add(picoStandardsTransferCol2Jaxb);
            standardsTransferMessage.getPlateTransferEvent().add(picoStandardsTransferCol4Jaxb);
            standardsTransferMessage.getPlateTransferEvent().add(picoStandardsTransferCol6Jaxb);
            standardsTransferMessage.getPlateTransferEvent().add(picoStandardsTransferCol8Jaxb);
            standardsTransferMessage.getPlateTransferEvent().add(picoStandardsTransferCol10Jaxb);
            standardsTransferMessage.getPlateTransferEvent().add(picoStandardsTransferCol12Jaxb);
            messageList.add(standardsTransferMessage);
        }

        /**
         * Build an entity graph for database free testing
         */
        void buildEntities() {
            buildJaxb();

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
            LabEventHandler labEventHandler = new LabEventHandler();

            LabEvent picoDilutionTransferEntityA1 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    picoDilutionTransferJaxbA1, mapBarcodeToTube, null);
            labEventHandler.processEvent(picoDilutionTransferEntityA1);
            StaticPlate dilutionPlate = (StaticPlate) picoDilutionTransferEntityA1.getTargetLabVessels().iterator().next();
            LabEvent picoDilutionTransferEntityA2 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    picoDilutionTransferJaxbA2, mapBarcodeToTube, dilutionPlate);
            labEventHandler.processEvent(picoDilutionTransferEntityA2);
            LabEvent picoDilutionTransferEntityB1 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    picoDilutionTransferJaxbB1, mapBarcodeToTube, dilutionPlate);
            labEventHandler.processEvent(picoDilutionTransferEntityB1);

            LabEvent picoMicroflourTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoMicroflourTransferJaxb, dilutionPlate, null);
            labEventHandler.processEvent(picoMicroflourTransferEntity);
            StaticPlate microflourPlate = (StaticPlate) picoMicroflourTransferEntity.getTargetLabVessels().iterator().next();

            StaticPlate picoStandardsPlate = new StaticPlate("PicoStandardsPlate", StaticPlate.PlateType.Eppendorf96);
            LabEvent picoStandardsTransferCol2Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol2Jaxb, picoStandardsPlate, microflourPlate);
            labEventHandler.processEvent(picoStandardsTransferCol2Entity);

            LabEvent picoStandardsTransferCol4Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol4Jaxb, picoStandardsPlate, microflourPlate);
            labEventHandler.processEvent(picoStandardsTransferCol4Entity);

            LabEvent picoStandardsTransferCol6Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol6Jaxb, picoStandardsPlate, microflourPlate);
            labEventHandler.processEvent(picoStandardsTransferCol6Entity);

            LabEvent picoStandardsTransferCol8Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol8Jaxb, picoStandardsPlate, microflourPlate);
            labEventHandler.processEvent(picoStandardsTransferCol8Entity);

            LabEvent picoStandardsTransferCol10Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol10Jaxb, picoStandardsPlate, microflourPlate);
            labEventHandler.processEvent(picoStandardsTransferCol10Entity);

            LabEvent picoStandardsTransferCol12Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol12Jaxb, picoStandardsPlate, microflourPlate);
            labEventHandler.processEvent(picoStandardsTransferCol12Entity);

//            Assert.assertEquals("Wrong number of sample instances", mapBarcodeToTube.size(),
//                    microflourPlate.getSampleInstances().size());
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }
    }
}
