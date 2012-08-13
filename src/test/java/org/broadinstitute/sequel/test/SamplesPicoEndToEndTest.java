package org.broadinstitute.sequel.test;

import junit.framework.Assert;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.sequel.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.sequel.boundary.labevent.LabEventBean;
import org.broadinstitute.sequel.boundary.labevent.LabEventResource;
import org.broadinstitute.sequel.boundary.labevent.LabVesselBean;
import org.broadinstitute.sequel.boundary.vessel.LabBatchBean;
import org.broadinstitute.sequel.boundary.vessel.LabBatchResource;
import org.broadinstitute.sequel.boundary.vessel.TubeBean;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.vessel.SBSSection;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselGeometry;
import org.broadinstitute.sequel.entity.workflow.LabBatch;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test BSP Pico messaging
 */
public class SamplesPicoEndToEndTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testAll() {
        // import batch, workflow and tubes
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for(int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String barcode = "R" + rackPosition;
            tubeBeans.add(new TubeBean(barcode, null));
        }
        String batchId = "BP-1";
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        Map<String, BSPStartingSample> mapBarcodeToSample = new LinkedHashMap<String, BSPStartingSample>();
        LabBatch labBatch = labBatchResource.buildLabBatch(new LabBatchBean(batchId, "HybSel", tubeBeans),
                mapBarcodeToTube, mapBarcodeToSample, null);

        // validate workflow?
        // messaging
        SamplesPicoMessageBuilder samplesPicoMessageBuilder = new SamplesPicoMessageBuilder(mapBarcodeToTube,
                labBatch);
        samplesPicoMessageBuilder.buildEntities();

        LabEventResource labEventResource = new LabEventResource();
        List<LabEventBean> labEventBeans = labEventResource.buildLabEventBeans(labBatch.getLabEvents());
        Assert.assertEquals("Wrong number of messages", 10, labEventBeans.size());
        LabEventBean standardsTransferEvent = labEventBeans.get(labEventBeans.size() - 1);
        LabVesselBean microfluorPlate = standardsTransferEvent.getTargets().iterator().next();
        Assert.assertEquals("Wrong barcode", samplesPicoMessageBuilder.getPicoMicrofluorTransferJaxb().getPlate().getBarcode(),
                microfluorPlate.getBarcode());
        Assert.assertEquals("Wrong starter", mapBarcodeToTube.values().iterator().next().getLabel(),
                microfluorPlate.getMapPositionToLabVessel().get("A01").getStarter());

        Set<String> barcodes = new HashSet<String>();
        for (LabEventBean labEventBean : labEventBeans) {
            for (LabVesselBean labVesselBean : labEventBean.getSources()) {
                if(barcodes.add(labVesselBean.getBarcode())) {
                    printVessel(labVesselBean);
                }
            }
            for (LabVesselBean labVesselBean : labEventBean.getTargets()) {
                if(barcodes.add(labVesselBean.getBarcode())) {
                    printVessel(labVesselBean);
                }
            }
        }

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
        private PlateTransferEventType picoMicrofluorTransferJaxb;
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
            picoDilutionTransferJaxbA1.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoDilutionTransferJaxbA1.setBatchId(labBatch.getBatchName());

            picoDilutionTransferJaxbA2 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA2.getPlate().setSection(SBSSection.A2.getSectionName());
            picoDilutionTransferJaxbA2.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoDilutionTransferJaxbA2.setBatchId(labBatch.getBatchName());

            picoDilutionTransferJaxbB1 = bettaLimsMessageFactory.buildRackToPlate(
                    "PicoDilutionTransfer", "PicoRack", new ArrayList<String>(mapBarcodeToTube.keySet()),
                    picoDilutionPlateBarcode);
            picoDilutionTransferJaxbB1.getPlate().setSection(SBSSection.B1.getSectionName());
            picoDilutionTransferJaxbB1.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
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
            // PicoMicrofluorTransfer
            String picoMicrofluorPlateBarcode = "PicoMicrofluorPlate";
            picoMicrofluorTransferJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoMicrofluorTransfer", picoDilutionPlateBarcode, picoMicrofluorPlateBarcode);
            picoMicrofluorTransferJaxb.getSourcePlate().setSection(SBSSection.ALL384.getSectionName());
            picoMicrofluorTransferJaxb.getSourcePlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoMicrofluorTransferJaxb.getPlate().setSection(SBSSection.ALL384.getSectionName());
            picoMicrofluorTransferJaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            // todo jmt batch ID is set only for the first message?
            picoMicrofluorTransferJaxb.setBatchId(labBatch.getBatchName());

            BettaLIMSMessage microfluorTransferMessage = new BettaLIMSMessage();
            microfluorTransferMessage.getPlateTransferEvent().add(picoMicrofluorTransferJaxb);
            messageList.add(microfluorTransferMessage);

            // 6 x PicoStandardsTransfer
            picoStandardsTransferCol2Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicrofluorPlateBarcode);
            picoStandardsTransferCol2Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol2Jaxb.getPlate().setSection(SBSSection.P384_COL2_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol2Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol2Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol4Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicrofluorPlateBarcode);
            picoStandardsTransferCol4Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol4Jaxb.getPlate().setSection(SBSSection.P384_COL4_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol4Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol4Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol6Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicrofluorPlateBarcode);
            picoStandardsTransferCol6Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol6Jaxb.getPlate().setSection(SBSSection.P384_COL6_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol6Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol6Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol8Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicrofluorPlateBarcode);
            picoStandardsTransferCol8Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol8Jaxb.getPlate().setSection(SBSSection.P384_COL8_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol8Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol8Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol10Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicrofluorPlateBarcode);
            picoStandardsTransferCol10Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol10Jaxb.getPlate().setSection(SBSSection.P384_COL10_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol10Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol10Jaxb.setBatchId(labBatch.getBatchName());

            picoStandardsTransferCol12Jaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PicoStandardsTransfer", "PicoStandardsPlate", picoMicrofluorPlateBarcode);
            picoStandardsTransferCol12Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol12Jaxb.getPlate().setSection(SBSSection.P384_COL12_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol12Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
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

            LabEvent picoMicrofluorTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoMicrofluorTransferJaxb, dilutionPlate, null);
            labEventHandler.processEvent(picoMicrofluorTransferEntity);
            StaticPlate microfluorPlate = (StaticPlate) picoMicrofluorTransferEntity.getTargetLabVessels().iterator().next();

            StaticPlate picoStandardsPlate = new StaticPlate("PicoStandardsPlate", StaticPlate.PlateType.Eppendorf96);
            LabEvent picoStandardsTransferCol2Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol2Jaxb, picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol2Entity);

            LabEvent picoStandardsTransferCol4Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol4Jaxb, picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol4Entity);

            LabEvent picoStandardsTransferCol6Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol6Jaxb, picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol6Entity);

            LabEvent picoStandardsTransferCol8Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol8Jaxb, picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol8Entity);

            LabEvent picoStandardsTransferCol10Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol10Jaxb, picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol10Entity);

            LabEvent picoStandardsTransferCol12Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    picoStandardsTransferCol12Jaxb, picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol12Entity);

//            Assert.assertEquals("Wrong number of sample instances", mapBarcodeToTube.size(),
//                    microfluorPlate.getSampleInstances().size());
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public PlateTransferEventType getPicoMicrofluorTransferJaxb() {
            return picoMicrofluorTransferJaxb;
        }
    }

    void printVessel(LabVesselBean labVesselBean) {
        StaticPlate.PlateType plateType = StaticPlate.PlateType.getByDisplayName(labVesselBean.getType());
        VesselGeometry vesselGeometry;
        if(plateType == null) {
            vesselGeometry = VesselGeometry.G12x8;
        } else {
            vesselGeometry = plateType.getVesselGeometry();
        }
        System.out.println(labVesselBean.getBarcode() + " " + labVesselBean.getType());
        System.out.print("    ");
        for (String columnName : vesselGeometry.getColumnNames()) {
            System.out.print(columnName + " ");
        }
        System.out.println();
        for (String rowName : vesselGeometry.getRowNames()) {
            System.out.print(rowName + " ");
            for (String columnName : vesselGeometry.getColumnNames()) {
                String starter = labVesselBean.getMapPositionToLabVessel().get(rowName + columnName).getStarter();
                if(starter == null) {
                    starter = "   ";
                }
                System.out.print(starter + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
