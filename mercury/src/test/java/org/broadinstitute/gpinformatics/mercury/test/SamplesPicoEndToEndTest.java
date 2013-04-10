package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.testng.Assert;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabVesselPositionBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.easymock.EasyMock;
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
        // import batch and tubes
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<TubeBean>();
        for (int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String barcode = "R" + rackPosition;
            tubeBeans.add(new TubeBean(barcode, null));
        }
        String batchId = "BP-1";
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        Map<MercurySample, MercurySample> mapSampleToSample = new LinkedHashMap<MercurySample, MercurySample>();
        LabBatch labBatch = labBatchResource.buildLabBatch(new LabBatchBean(batchId, "HybSel", tubeBeans),
                                                           mapBarcodeToTube, mapSampleToSample/*, null*/);

        // validate workflow?
        // messaging
        SamplesPicoJaxbBuilder samplesPicoJaxbBuilder = new SamplesPicoJaxbBuilder(new ArrayList<String>(
                mapBarcodeToTube.keySet()), labBatch.getBatchName(), "");
        SamplesPicoEntityBuilder samplesPicoEntityBuilder = new SamplesPicoEntityBuilder(samplesPicoJaxbBuilder,
                                                                                         labBatch, mapBarcodeToTube);
        samplesPicoEntityBuilder.buildEntities();

        // event web service, by batch
        LabEventResource labEventResource = new LabEventResource();
        List<LabEventBean> labEventBeans = labEventResource.buildLabEventBeans(new ArrayList<LabEvent>(
                labBatch.getLabEvents()),
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
        Assert.assertEquals(10, labEventBeans.size(), "Wrong number of messages");
        LabEventBean standardsTransferEvent = labEventBeans.get(labEventBeans.size() - 1);
        LabVesselBean microfluorPlate = standardsTransferEvent.getTargets().iterator().next();
        Assert.assertEquals(samplesPicoJaxbBuilder.getPicoMicrofluorTransferJaxb().getPlate().getBarcode(),
                            microfluorPlate.getBarcode(), "Wrong barcode");
        LabVesselPositionBean labVesselPositionBean = microfluorPlate.getLabVesselPositionBeans().get(0);
        Assert.assertEquals("A01", labVesselPositionBean.getPosition(), "Wrong position");
        Assert.assertEquals(mapBarcodeToTube.values().iterator().next().getLabel(),
                            labVesselPositionBean.getLabVesselBean().getStarter(), "Wrong starter");

        printLabEvents(labEventBeans);

        // transfer visualizer?
        // datamart?
    }

    public static void printLabEvents(List<LabEventBean> labEventBeans) {
        Set<String> barcodes = new HashSet<String>();
        for (LabEventBean labEventBean : labEventBeans) {
            System.out.println(labEventBean.getEventType() + " " + labEventBean.getEventDate());
            for (LabVesselBean labVesselBean : labEventBean.getSources()) {
                if (barcodes.add(labVesselBean.getBarcode())) {
                    printVessel(labVesselBean);
                }
            }
            for (LabVesselBean labVesselBean : labEventBean.getTargets()) {
                if (barcodes.add(labVesselBean.getBarcode())) {
                    printVessel(labVesselBean);
                }
            }
        }
    }

    /**
     * Build the messages used in Samples (BSP) Pico batches
     */
    @SuppressWarnings("FeatureEnvy")
    public static class SamplesPicoJaxbBuilder {
        private final List<String> tubeBarcodes;
        private final String       labBatchId;
        private final String       timestamp;

        private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
        private PlateTransferEventType picoDilutionTransferJaxbA1;
        private PlateTransferEventType picoDilutionTransferJaxbA2;
        private PlateTransferEventType picoDilutionTransferJaxbB1;
        private PlateEventType         picoBufferAdditionJaxb;
        private PlateTransferEventType picoMicrofluorTransferJaxb;
        private PlateTransferEventType picoStandardsTransferCol2Jaxb;
        private PlateTransferEventType picoStandardsTransferCol4Jaxb;
        private PlateTransferEventType picoStandardsTransferCol6Jaxb;
        private PlateTransferEventType picoStandardsTransferCol8Jaxb;
        private PlateTransferEventType picoStandardsTransferCol10Jaxb;
        private PlateTransferEventType picoStandardsTransferCol12Jaxb;

        SamplesPicoJaxbBuilder(List<String> tubeBarcodes, String labBatchId, String timestamp) {
            this.tubeBarcodes = tubeBarcodes;
            this.labBatchId = labBatchId;
            this.timestamp = timestamp;
        }

        /**
         * Build JAXB messages.  These messages can be sent to BettalimsMessageResource, or used to build entity
         * graphs.
         */
        void buildJaxb() {
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory();

            // 3 x  PicoDilutionTransfer
            String picoDilutionPlateBarcode = "PicoDilutionPlate" + timestamp;
            picoDilutionTransferJaxbA1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                    "PicoRack"  + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA1.getPlate().setSection(SBSSection.P384_96TIP_1INTERVAL_A1.getSectionName());
            picoDilutionTransferJaxbA1.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoDilutionTransferJaxbA1.setBatchId(labBatchId);

            picoDilutionTransferJaxbA2 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                    "PicoRack" + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
            picoDilutionTransferJaxbA2.getPlate().setSection(SBSSection.P384_96TIP_1INTERVAL_A2.getSectionName());
            picoDilutionTransferJaxbA2.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoDilutionTransferJaxbA2.setBatchId(labBatchId);

            picoDilutionTransferJaxbB1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                    "PicoRack"  + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
            picoDilutionTransferJaxbB1.getPlate().setSection(SBSSection.P384_96TIP_1INTERVAL_B1.getSectionName());
            picoDilutionTransferJaxbB1.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoDilutionTransferJaxbB1.setBatchId(labBatchId);

            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoDilutionTransferJaxbA1,
                    picoDilutionTransferJaxbA2, picoDilutionTransferJaxbB1);

            /*
                        // PicoBufferTransfer
                        PlateTransferEventType picoBufferTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                                "PicoBufferTransfer", "PicoBufferPlate", picoDilutionPlateBarcode);
                        BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
                        bettaLIMSMessage2.getPlateTransferEvent().add(picoBufferTransferJaxb);
                        messageList.add(bettaLIMSMessage2);

            */
            // plateEvent PicoBufferAddition
            picoBufferAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition",
                                                                             picoDilutionPlateBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoBufferAdditionJaxb);

            // PicoMicrofluorTransfer
            String picoMicrofluorPlateBarcode = "PicoMicrofluorPlate" + timestamp;
            picoMicrofluorTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoMicrofluorTransfer",
                                                                                   picoDilutionPlateBarcode,
                                                                                   picoMicrofluorPlateBarcode);
            picoMicrofluorTransferJaxb.getSourcePlate().setSection(SBSSection.ALL384.getSectionName());
            picoMicrofluorTransferJaxb.getSourcePlate().setPhysType(
                    StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoMicrofluorTransferJaxb.getPlate().setSection(SBSSection.ALL384.getSectionName());
            picoMicrofluorTransferJaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            // todo jmt batch ID is set only for the first message?
            picoMicrofluorTransferJaxb.setBatchId(labBatchId);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoMicrofluorTransferJaxb);

            // 6 x PicoStandardsTransfer
            picoStandardsTransferCol2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                    "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
            picoStandardsTransferCol2Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol2Jaxb.getPlate().setSection(SBSSection.P384_COL2_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol2Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol2Jaxb.setBatchId(labBatchId);

            picoStandardsTransferCol4Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                    "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
            picoStandardsTransferCol4Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol4Jaxb.getPlate().setSection(SBSSection.P384_COL4_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol4Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol4Jaxb.setBatchId(labBatchId);

            picoStandardsTransferCol6Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                    "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
            picoStandardsTransferCol6Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol6Jaxb.getPlate().setSection(SBSSection.P384_COL6_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol6Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol6Jaxb.setBatchId(labBatchId);

            picoStandardsTransferCol8Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                    "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
            picoStandardsTransferCol8Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol8Jaxb.getPlate().setSection(SBSSection.P384_COL8_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol8Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol8Jaxb.setBatchId(labBatchId);

            picoStandardsTransferCol10Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                    "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
            picoStandardsTransferCol10Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol10Jaxb.getPlate().setSection(SBSSection.P384_COL10_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol10Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol10Jaxb.setBatchId(labBatchId);

            picoStandardsTransferCol12Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                    "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
            picoStandardsTransferCol12Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
            picoStandardsTransferCol12Jaxb.getPlate().setSection(SBSSection.P384_COL12_1INTERVAL_B.getSectionName());
            picoStandardsTransferCol12Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getDisplayName());
            picoStandardsTransferCol12Jaxb.setBatchId(labBatchId);

            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoStandardsTransferCol2Jaxb,
                    picoStandardsTransferCol4Jaxb, picoStandardsTransferCol6Jaxb, picoStandardsTransferCol8Jaxb,
                    picoStandardsTransferCol10Jaxb, picoStandardsTransferCol12Jaxb);
        }

        public List<BettaLIMSMessage> getMessageList() {
            return messageList;
        }

        public PlateTransferEventType getPicoMicrofluorTransferJaxb() {
            return picoMicrofluorTransferJaxb;
        }

        public PlateTransferEventType getPicoDilutionTransferJaxbA1() {
            return picoDilutionTransferJaxbA1;
        }

        public PlateTransferEventType getPicoDilutionTransferJaxbA2() {
            return picoDilutionTransferJaxbA2;
        }

        public PlateTransferEventType getPicoDilutionTransferJaxbB1() {
            return picoDilutionTransferJaxbB1;
        }

        public PlateTransferEventType getPicoStandardsTransferCol2Jaxb() {
            return picoStandardsTransferCol2Jaxb;
        }

        public PlateTransferEventType getPicoStandardsTransferCol4Jaxb() {
            return picoStandardsTransferCol4Jaxb;
        }

        public PlateTransferEventType getPicoStandardsTransferCol6Jaxb() {
            return picoStandardsTransferCol6Jaxb;
        }

        public PlateTransferEventType getPicoStandardsTransferCol8Jaxb() {
            return picoStandardsTransferCol8Jaxb;
        }

        public PlateTransferEventType getPicoStandardsTransferCol10Jaxb() {
            return picoStandardsTransferCol10Jaxb;
        }

        public PlateTransferEventType getPicoStandardsTransferCol12Jaxb() {
            return picoStandardsTransferCol12Jaxb;
        }
    }

    public static class SamplesPicoEntityBuilder {

        private final SamplesPicoJaxbBuilder        samplesPicoJaxbBuilder;
        private final LabBatch                      labBatch;
        private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;

        public SamplesPicoEntityBuilder(SamplesPicoJaxbBuilder samplesPicoJaxbBuilder, LabBatch labBatch,
                                        Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
            this.samplesPicoJaxbBuilder = samplesPicoJaxbBuilder;
            this.labBatch = labBatch;
            this.mapBarcodeToTube = mapBarcodeToTube;
        }

        /**
         * Build an entity graph for database free testing
         */
        void buildEntities() {
            samplesPicoJaxbBuilder.buildJaxb();

            LabEventFactory labEventFactory = new LabEventFactory();
            labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
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

            LabBatchEjb labBatchEJB = new LabBatchEjb();
            labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
            labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

            LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
            labBatchEJB.setTubeDAO(tubeDao);

            JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
            labBatchEJB.setJiraTicketDao(mockJira);

            LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
            labBatchEJB.setLabBatchDao(labBatchDAO);



            BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
            ReworkEjb reworkEjb = EasyMock.createNiceMock(ReworkEjb.class);
            BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB
            );
            EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

            TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.postConstruct();
            LabEventHandler labEventHandler = new LabEventHandler(new WorkflowLoader(),
                    AthenaClientProducer.stubInstance(), bucketBeanEJB, mockBucketDao,
                    new BSPUserList(BSPManagerFactoryProducer.stubInstance()));

            LabEvent picoDilutionTransferEntityA1 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoDilutionTransferJaxbA1(), mapBarcodeToTube, null, null);
            labEventHandler.processEvent(picoDilutionTransferEntityA1);
            StaticPlate dilutionPlate =
                    (StaticPlate) picoDilutionTransferEntityA1.getTargetLabVessels().iterator().next();
            LabEvent picoDilutionTransferEntityA2 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoDilutionTransferJaxbA2(), mapBarcodeToTube, null, dilutionPlate);
            labEventHandler.processEvent(picoDilutionTransferEntityA2);
            LabEvent picoDilutionTransferEntityB1 = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoDilutionTransferJaxbB1(), mapBarcodeToTube, null, dilutionPlate);
            labEventHandler.processEvent(picoDilutionTransferEntityB1);

            LabEvent picoMicrofluorTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoMicrofluorTransferJaxb(), dilutionPlate, null);
            labEventHandler.processEvent(picoMicrofluorTransferEntity);
            StaticPlate microfluorPlate =
                    (StaticPlate) picoMicrofluorTransferEntity.getTargetLabVessels().iterator().next();

            StaticPlate picoStandardsPlate = new StaticPlate("PicoStandardsPlate", StaticPlate.PlateType.Eppendorf96);
            LabEvent picoStandardsTransferCol2Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol2Jaxb(), picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol2Entity);

            LabEvent picoStandardsTransferCol4Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol4Jaxb(), picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol4Entity);

            LabEvent picoStandardsTransferCol6Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol6Jaxb(), picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol6Entity);

            LabEvent picoStandardsTransferCol8Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol8Jaxb(), picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol8Entity);

            LabEvent picoStandardsTransferCol10Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol10Jaxb(), picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol10Entity);

            LabEvent picoStandardsTransferCol12Entity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol12Jaxb(), picoStandardsPlate, microfluorPlate);
            labEventHandler.processEvent(picoStandardsTransferCol12Entity);

            //            Assert.assertEquals("Wrong number of sample instances", mapBarcodeToTube.size(),
            //                    microfluorPlate.getSampleInstances().size());


        }
    }

    public static void printVessel(LabVesselBean labVesselBean) {
        StaticPlate.PlateType plateType = StaticPlate.PlateType.getByDisplayName(labVesselBean.getType());
        VesselGeometry vesselGeometry;
        if (plateType == null) {
            vesselGeometry = VesselGeometry.G12x8;
        } else {
            vesselGeometry = plateType.getVesselGeometry();
        }
        System.out.println(labVesselBean.getBarcode() + " " + labVesselBean.getType());
        System.out.print("  ");
        for (String columnName : vesselGeometry.getColumnNames()) {
            System.out.print(columnName + "  ");
        }
        System.out.println();
        int positionIndex = 0;
        int maxPositionIndex = labVesselBean.getLabVesselPositionBeans().size() - 1;
        for (String rowName : vesselGeometry.getRowNames()) {
            System.out.print(rowName + " ");
            for (String columnName : vesselGeometry.getColumnNames()) {
                if(positionIndex > maxPositionIndex) {
                    break;
                }
                LabVesselPositionBean labVesselPositionBean = labVesselBean.getLabVesselPositionBeans().get(
                        positionIndex);
                String starter = labVesselPositionBean.getLabVesselBean().getStarter();
                if (starter == null) {
                    starter = "   ";
                }
                System.out.print(starter + " ");
                positionIndex++;
            }
            System.out.println();
        }
        System.out.println();
    }
}
