package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 3/14/13
 *         Time: 12:08 AM
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class SolexaRunRoutingTest {

    private static final int NUM_POSITIONS_IN_RACK = 96;

    private static Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();
    private BucketBean                    bucketBeanEJB;
    private BucketDao                     mockBucketDao;
    private BettaLimsMessageFactory       bettaLimsMessageFactory;
    private LabEventFactory               labEventFactory;
    private Map<String, TwoDBarcodedTube> mapBarcodeToTube;

    @BeforeMethod
    public void setUp() {

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        ProductOrder productOrder = AthenaClientServiceStub.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        String jiraTicketKey = productOrder.getBusinessKey();

        mapKeyToProductOrder.put(jiraTicketKey, productOrder);

        // starting rack
        mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            rackPosition++;
        }
        final LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                                                           new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                                           LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(workflowBatch, "scottmat");

        bettaLimsMessageFactory = new BettaLimsMessageFactory();
        labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
            @Override
            public BspUser getOperator(String userId) {

                BspUser tester = new BspUser();
                tester.setUsername(userId);
                tester.setBadgeNumber("24242342423");
                tester.setUserId(101L);
                tester.setFirstName("Howie");
                tester.setLastName("Mandel");

                return tester;
            }

            @Override
            public BspUser getOperator(Long bspUserId) {
                BspUser tester = new BspUser();
                tester.setUsername("hrafal");
                tester.setBadgeNumber("24242342423");
                tester.setUserId(bspUserId);
                tester.setFirstName("Howie");
                tester.setLastName("Mandel");

                return tester;
            }

            @Override
            public LabBatch getLabBatch(String labBatchName) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);
    }

    /**
     * For a Whole Genome Shotgun flowcell, Squid will be the only system that should receive run registration.
     * This method will take tubes through the entire Whole Genome Shotgun workflow and attempt to register it through
     * Mercury.  If at anytime the service attempts to register within Mercury, a failure ensues.
     *
     * @throws Exception
     */
    public void testWholeGenomeFlowcell() throws Exception {

        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer
                                                                  .stubInstance(), bucketBeanEJB, mockBucketDao,
                                           new BSPUserList(BSPManagerFactoryProducer
                                                                   .stubInstance()));

        LabEventTest.PreFlightEntityBuilder preFlightEntityBuilder =
                new LabEventTest.PreFlightEntityBuilder(bettaLimsMessageFactory,
                                                               labEventFactory, labEventHandler,
                                                               mapBarcodeToTube, mapKeyToProductOrder).invoke();

        LabEventTest.ShearingEntityBuilder shearingEntityBuilder =
                new LabEventTest.ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                                                              bettaLimsMessageFactory, labEventFactory, labEventHandler,
                                                              preFlightEntityBuilder.getRackBarcode()).invoke();

        LabEventTest.LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                new LabEventTest.LibraryConstructionEntityBuilder(
                                                                         bettaLimsMessageFactory, labEventFactory,
                                                                         labEventHandler,
                                                                         shearingEntityBuilder
                                                                                 .getShearingCleanupPlate(),
                                                                         shearingEntityBuilder
                                                                                 .getShearCleanPlateBarcode(),
                                                                         shearingEntityBuilder.getShearingPlate(),
                                                                         NUM_POSITIONS_IN_RACK).invoke();

        List<String> sageUnloadTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadTubeBarcodes.add("SageUnload" + i);
        }
        String sageUnloadBarcode = "SageUnload";
        Map<String, TwoDBarcodedTube> mapBarcodeToSageUnloadTubes = new HashMap<String, TwoDBarcodedTube>();
        RackOfTubes targetRackOfTubes = null;
        for (int i = 0; i < NUM_POSITIONS_IN_RACK / 4; i++) {
            // SageLoading
            String sageCassetteBarcode = "SageCassette" + i;
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageFactory.buildRackToPlate("SageLoading",
                                                                                                     libraryConstructionEntityBuilder
                                                                                                             .getPondRegRackBarcode(),
                                                                                                     libraryConstructionEntityBuilder
                                                                                                             .getPondRegTubeBarcodes()
                                                                                                             .subList(i * 4,
                                                                                                                             i * 4 + 4),
                                                                                                     sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb,
                                                                                                    libraryConstructionEntityBuilder
                                                                                                            .getPondRegRack(),
                                                                                                    null);
            labEventHandler.processEvent(sageLoadingEntity);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageFactory.buildPlateToRack("SageUnloading",
                                                                                                       sageCassetteBarcode,
                                                                                                       sageUnloadBarcode,
                                                                                                       sageUnloadTubeBarcodes
                                                                                                               .subList(i * 4,
                                                                                                                               i * 4 + 4));
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(sageUnloadingJaxb,
                                                                                                   sageCassette,
                                                                                                   mapBarcodeToSageUnloadTubes,
                                                                                                   targetRackOfTubes);
            labEventHandler.processEvent(sageUnloadEntity);
            sageUnloadEntity.getTargetLabVessels().iterator().next();
        }

        // SageCleanup
        List<String> sageCleanupTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageCleanupTubeBarcodes.add("SageCleanup" + i);
        }
        String sageCleanupBarcode = "SageCleanup";
        PlateTransferEventType sageCleanupJaxb =
                bettaLimsMessageFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                                                               sageUnloadTubeBarcodes, sageCleanupBarcode,
                                                               sageCleanupTubeBarcodes);
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            mapPositionToTube.put(VesselPosition
                                          .getByName(bettaLimsMessageFactory.buildWellName(i + 1)),
                                         sageUnloadTubes.get(i));
        }
        TubeFormation sageUnloadRackRearrayed = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        sageUnloadRackRearrayed.addRackOfTubes(new RackOfTubes("sageUnloadRearray", RackOfTubes.RackType.Matrix96));
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                                                                                               sageUnloadRackRearrayed,
                                                                                               new HashMap<String, TwoDBarcodedTube>(),
                                                                                               targetRackOfTubes);
        labEventHandler.processEvent(sageCleanupEntity);
        TubeFormation sageCleanupRack = (TubeFormation) sageCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(sageCleanupRack.getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                                   "Wrong number of sage cleanup samples");

        LabEventTest.QtpEntityBuilder qtpEntityBuilder =
                new LabEventTest.QtpEntityBuilder(bettaLimsMessageFactory, labEventFactory, labEventHandler,
                                                         sageCleanupRack,
                                                         sageCleanupBarcode, sageCleanupTubeBarcodes,
                                                         mapBarcodeToSageUnloadTubes);

        qtpEntityBuilder.invoke();

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue()
                                   .evaluateCriteria(transferTraverserCriteria,
                                                            TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();

        IlluminaFlowcell flowcell = qtpEntityBuilder.getIlluminaFlowcell();
        Date runDate = new Date();

        SolexaRunBean runBean =
                new SolexaRunBean(flowcell.getCartridgeBarcode(),
                                         flowcell.getCartridgeBarcode() + IlluminaSequencingRun.RUNFORMAT
                                                                                               .format(runDate),
                                         runDate, "Superman",
                                         File.createTempFile("tempRun" + IlluminaSequencingRun.RUNFORMAT
                                                                                              .format(runDate), ".txt")
                                             .getAbsolutePath(), null);

        IlluminaSequencingRunDao runDao = EasyMock.createMock(IlluminaSequencingRunDao.class);

        EasyMock.expect(runDao.findByRunName(EasyMock.anyObject(String.class))).andReturn(null);

        IlluminaSequencingRunFactory runFactory = EasyMock.createMock(IlluminaSequencingRunFactory.class);
        //        EasyMock.expect(runFactory.build(EasyMock.anyObject(SolexaRunBean.class),
        //                                                EasyMock.anyObject(IlluminaFlowcell.class)))
        ////                .andReturn(new IlluminaSequencingRun(null, null, null, null,null, false, null,null))
        //                .andThrow(new InformaticsServiceException("This is a Whole Genome Shotgun workflow.  " +
        //                                                                  "We should NOT have called Mercury"))
        ;

        IlluminaFlowcellDao flowcellDao = EasyMock.createNiceMock(IlluminaFlowcellDao.class);
        EasyMock.expect(flowcellDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(flowcell);

        LabVesselDao vesselDao = EasyMock.createNiceMock(LabVesselDao.class);

        MercuryOrSquidRouter router = new MercuryOrSquidRouter(vesselDao, AthenaClientProducer.stubInstance());

        SolexaRunResource runResource = new SolexaRunResource(runDao, runFactory, flowcellDao, router,
                                                                     SquidConnectorProducer.stubInstance());
        EasyMock.replay(runDao, runFactory, flowcellDao, vesselDao);

        runResource.createRun(runBean, new UriInfo() {
            @Override
            public String getPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getPath(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<PathSegment> getPathSegments() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<PathSegment> getPathSegments(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public URI getRequestUri() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public UriBuilder getRequestUriBuilder() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public URI getAbsolutePath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public UriBuilder getAbsolutePathBuilder() {
                return UriBuilder.fromPath("");
            }

            @Override
            public URI getBaseUri() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public UriBuilder getBaseUriBuilder() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getPathParameters() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getPathParameters(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getQueryParameters() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<String> getMatchedURIs() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<String> getMatchedURIs(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<Object> getMatchedResources() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        EasyMock.verify(runDao, flowcellDao, vesselDao, runFactory);

    }

    public void testNoChainOfCustodyRegistration() throws Exception {

        Date runDate = new Date();

        final String flowcellBarcode = "FlowCellBarcode" + runDate.getTime();
        SolexaRunBean runBean =
                new SolexaRunBean(flowcellBarcode,
                                         flowcellBarcode + IlluminaSequencingRun.RUNFORMAT.format(runDate),
                                         runDate, "Superman",
                                         File.createTempFile("tempRun" + IlluminaSequencingRun.RUNFORMAT
                                                                                              .format(runDate), ".txt")
                                             .getAbsolutePath(), null);

        IlluminaSequencingRunDao runDao = EasyMock.createMock(IlluminaSequencingRunDao.class);

        EasyMock.expect(runDao.findByRunName(EasyMock.anyObject(String.class))).andReturn(null);


        IlluminaSequencingRunFactory runFactory = EasyMock.createMock(IlluminaSequencingRunFactory.class);
        //        EasyMock.expect(runFactory.build(EasyMock.anyObject(SolexaRunBean.class),
        //                                                EasyMock.anyObject(IlluminaFlowcell.class)))
        ////                .andReturn(new IlluminaSequencingRun(null, null, null, null,null, false, null,null))
        //                .andThrow(new InformaticsServiceException("This is a Whole Genome Shotgun workflow.  " +
        //                                                                  "We should NOT have called Mercury"))
        ;

        IlluminaFlowcellDao flowcellDao = EasyMock.createNiceMock(IlluminaFlowcellDao.class);
        EasyMock.expect(flowcellDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(null);

        LabVesselDao vesselDao = EasyMock.createNiceMock(LabVesselDao.class);

        MercuryOrSquidRouter router = new MercuryOrSquidRouter(vesselDao, AthenaClientProducer.stubInstance());

        SolexaRunResource runResource = new SolexaRunResource(runDao, runFactory, flowcellDao, router,
                                                                     SquidConnectorProducer.stubInstance());
        EasyMock.replay(runDao, runFactory, flowcellDao, vesselDao);

        runResource.createRun(runBean, new UriInfo() {
            @Override
            public String getPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public String getPath(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<PathSegment> getPathSegments() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<PathSegment> getPathSegments(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public URI getRequestUri() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public UriBuilder getRequestUriBuilder() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public URI getAbsolutePath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public UriBuilder getAbsolutePathBuilder() {
                return UriBuilder.fromPath("");
            }

            @Override
            public URI getBaseUri() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public UriBuilder getBaseUriBuilder() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getPathParameters() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getPathParameters(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getQueryParameters() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<String> getMatchedURIs() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<String> getMatchedURIs(boolean decode) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public List<Object> getMatchedResources() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        EasyMock.verify(runDao, flowcellDao, vesselDao, runFactory);
    }
}
