package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.builders.SamplesPicoJaxbBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

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
            BucketEjb bucketEjb = new BucketEjb(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB
            );
            EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

            TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.postConstruct();
            LabEventHandler labEventHandler = new LabEventHandler(new WorkflowLoader(),
                    AthenaClientProducer.stubInstance());

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
