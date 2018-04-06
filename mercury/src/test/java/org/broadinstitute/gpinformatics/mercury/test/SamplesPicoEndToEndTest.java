package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabEventResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabVesselBean;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.LabVesselPositionBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.TubeBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventRefDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.builders.SamplesPicoJaxbBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test BSP Pico messaging
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SamplesPicoEndToEndTest {

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testAll() {
        // import batch and tubes
        LabBatchResource labBatchResource = new LabBatchResource();
        List<TubeBean> tubeBeans = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= 96; rackPosition++) {
            String barcode = "R" + rackPosition;
            tubeBeans.add(new TubeBean(barcode, null));
        }
        String batchId = "BP-1";
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        Map<MercurySample, MercurySample> mapSampleToSample = new LinkedHashMap<>();
        LabBatch labBatch = labBatchResource.buildLabBatch(new LabBatchBean(batchId, "HybSel", tubeBeans),
                                                           mapBarcodeToTube, mapSampleToSample/*, null*/);

        // validate workflow?
        // messaging
        SamplesPicoJaxbBuilder samplesPicoJaxbBuilder = new SamplesPicoJaxbBuilder(new ArrayList<>(
                mapBarcodeToTube.keySet()), labBatch.getBatchName(), "");
        SamplesPicoEntityBuilder samplesPicoEntityBuilder = new SamplesPicoEntityBuilder(samplesPicoJaxbBuilder,
                                                                                         labBatch, mapBarcodeToTube);
        samplesPicoEntityBuilder.buildEntities();

        // event web service, by batch
        LabEventResource labEventResource = new LabEventResource();
        List<LabEventBean> labEventBeans = labEventResource.buildLabEventBeans(new ArrayList<>(
                labBatch.getLabEvents()),
                                                                               new LabEventRefDataFetcher() {
                                                                                   @Override
                                                                                   public BspUser getOperator(
                                                                                           String userId) {
                                                                                       BSPUserList testList =
                                                                                               new BSPUserList(
                                                                                                       BSPManagerFactoryProducer
                                                                                                               .stubInstance());
                                                                                       return testList.getByUsername(
                                                                                               userId);
                                                                                   }

                                                                                   @Override
                                                                                   public BspUser getOperator(
                                                                                           Long bspUserId) {
                                                                                       BSPUserList testList =
                                                                                               new BSPUserList(
                                                                                                       BSPManagerFactoryProducer
                                                                                                               .stubInstance());
                                                                                       return testList.getById(
                                                                                               bspUserId);
                                                                                   }

                                                                                   @Override
                                                                                   public LabBatch getLabBatch(
                                                                                           String labBatchName) {
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

    static void printLabEvents(List<LabEventBean> labEventBeans) {
        Set<String> barcodes = new HashSet<>();
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

    public static class SamplesPicoEntityBuilder {

        private final SamplesPicoJaxbBuilder samplesPicoJaxbBuilder;
        private final LabBatch labBatch;
        private final Map<String, BarcodedTube> mapBarcodeToTube;

        public SamplesPicoEntityBuilder(SamplesPicoJaxbBuilder samplesPicoJaxbBuilder, LabBatch labBatch,
                                        Map<String, BarcodedTube> mapBarcodeToTube) {
            this.samplesPicoJaxbBuilder = samplesPicoJaxbBuilder;
            this.labBatch = labBatch;
            this.mapBarcodeToTube = mapBarcodeToTube;
        }

        /**
         * Build an entity graph for database free testing
         */
        void buildEntities() {
            samplesPicoJaxbBuilder.buildJaxb();

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

            LabBatchEjb labBatchEJB = new LabBatchEjb();
            labBatchEJB.setJiraService(JiraServiceTestProducer.stubInstance());

            LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
            labBatchEJB.setTubeDao(tubeDao);

            LabBatchDao labBatchDao = EasyMock.createNiceMock(LabBatchDao.class);
            labBatchEJB.setLabBatchDao(labBatchDao);

            ProductOrderDao mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
            Mockito.when(mockProductOrderDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                    Object[] arguments = invocationOnMock.getArguments();

                    return ProductOrderTestFactory.createDummyProductOrder((String) arguments[0]);
                }
            });
            labBatchEJB.setProductOrderDao(mockProductOrderDao);
            BucketDao bucketDao = EasyMock.createNiceMock(BucketDao.class);


            BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
            EasyMock.replay(mockBucketDao, tubeDao, labBatchDao, bucketDao);

            TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.postConstruct();
            LabEventHandler labEventHandler = new LabEventHandler();

            Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
            mapBarcodeToVessel.putAll(mapBarcodeToTube);
            LabEvent picoDilutionTransferEntityA1 = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoDilutionTransferJaxbA1(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoDilutionTransferEntityA1);
            StaticPlate dilutionPlate =
                    (StaticPlate) picoDilutionTransferEntityA1.getTargetLabVessels().iterator().next();

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.putAll(mapBarcodeToTube);
            LabEvent picoDilutionTransferEntityA2 = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoDilutionTransferJaxbA2(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoDilutionTransferEntityA2);

            LabEvent picoDilutionTransferEntityB1 = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoDilutionTransferJaxbB1(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoDilutionTransferEntityB1);

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(dilutionPlate.getLabel(), dilutionPlate);
            LabEvent picoMicrofluorTransferEntity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoMicrofluorTransferJaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoMicrofluorTransferEntity);
            StaticPlate microfluorPlate =
                    (StaticPlate) picoMicrofluorTransferEntity.getTargetLabVessels().iterator().next();

            StaticPlate picoStandardsPlate = new StaticPlate("PicoStandardsPlate", StaticPlate.PlateType.Eppendorf96);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(picoStandardsPlate.getLabel(), picoStandardsPlate);
            mapBarcodeToVessel.put(microfluorPlate.getLabel(), microfluorPlate);
            LabEvent picoStandardsTransferCol2Entity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol2Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoStandardsTransferCol2Entity);

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(picoStandardsPlate.getLabel(), picoStandardsPlate);
            mapBarcodeToVessel.put(microfluorPlate.getLabel(), microfluorPlate);
            LabEvent picoStandardsTransferCol4Entity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol4Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoStandardsTransferCol4Entity);

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(picoStandardsPlate.getLabel(), picoStandardsPlate);
            mapBarcodeToVessel.put(microfluorPlate.getLabel(), microfluorPlate);
            LabEvent picoStandardsTransferCol6Entity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol6Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoStandardsTransferCol6Entity);

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(picoStandardsPlate.getLabel(), picoStandardsPlate);
            mapBarcodeToVessel.put(microfluorPlate.getLabel(), microfluorPlate);
            LabEvent picoStandardsTransferCol8Entity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol8Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoStandardsTransferCol8Entity);

            mapBarcodeToVessel.put(picoStandardsPlate.getLabel(), picoStandardsPlate);
            mapBarcodeToVessel.put(microfluorPlate.getLabel(), microfluorPlate);
            LabEvent picoStandardsTransferCol10Entity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol10Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoStandardsTransferCol10Entity);

            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(picoStandardsPlate.getLabel(), picoStandardsPlate);
            mapBarcodeToVessel.put(microfluorPlate.getLabel(), microfluorPlate);
            LabEvent picoStandardsTransferCol12Entity = labEventFactory.buildFromBettaLims(
                    samplesPicoJaxbBuilder.getPicoStandardsTransferCol12Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(picoStandardsTransferCol12Entity);

            //            Assert.assertEquals("Wrong number of sample instances", mapBarcodeToTube.size(),
            //                    microfluorPlate.getSampleInstances().size());


        }
    }

    private static void printVessel(LabVesselBean labVesselBean) {
        StaticPlate.PlateType plateType = StaticPlate.PlateType.getByAutomationName(labVesselBean.getType());
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
                if (positionIndex > maxPositionIndex) {
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
