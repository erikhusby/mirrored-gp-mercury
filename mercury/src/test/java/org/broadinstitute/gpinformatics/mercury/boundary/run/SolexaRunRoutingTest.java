package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.BSPExportsService;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.MiSeqReagentKitEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.SageEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingEntityBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class SolexaRunRoutingTest extends BaseEventTest {

    private static final int NUM_POSITIONS_IN_RACK = 96;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);
    private String reagentBlockBarcode;

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
    }

    /**
     * For a Whole Genome Shotgun flowcell, Squid will be the only system that should receive run registration.
     * This method will take tubes through the entire Whole Genome Shotgun workflow and attempt to register it through
     * Mercury.  If at anytime the service attempts to register within Mercury, a failure ensues.
     *
     * @throws Exception
     */
    public void testWholeGenomeFlowcell() throws Exception {
        expectedRouting = SystemRouter.System.SQUID;

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        final ProductOrder productOrder =
                ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.WHOLE_GENOME);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PreFlightEntityBuilder preFlightEntityBuilder = runPreflightProcess(mapBarcodeToTube, "1");
        ShearingEntityBuilder shearingEntityBuilder =
                runShearingProcess(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                        preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(shearingEntityBuilder.getShearingCleanupPlate(),
                        shearingEntityBuilder.getShearCleanPlateBarcode(), shearingEntityBuilder.getShearingPlate(),
                        "1");
        SageEntityBuilder sageEntityBuilder = runSageProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes());

        Assert.assertEquals(sageEntityBuilder.getSageCleanupRack().getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder =
                runQtpProcess(sageEntityBuilder.getSageCleanupRack(), sageEntityBuilder.getSageCleanupTubeBarcodes(),
                        sageEntityBuilder.getMapBarcodeToSageUnloadTubes(), "1");

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1", null,
                        ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "designation", Workflow.WHOLE_GENOME);

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        @SuppressWarnings("UnusedDeclaration")
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();

        IlluminaFlowcell flowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        Date runDate = new Date();

        SolexaRunBean runBean =
                new SolexaRunBean(flowcell.getCartridgeBarcode(),
                        flowcell.getCartridgeBarcode() + dateFormat.format(runDate),
                        runDate, "Superman",
                        File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt")
                                .getAbsolutePath(), null);

        IlluminaSequencingRunDao runDao = EasyMock.createMock(IlluminaSequencingRunDao.class);

        EasyMock.expect(runDao.findByRunName(EasyMock.anyObject(String.class))).andReturn(null);

        IlluminaSequencingRunFactory runFactory = EasyMock.createMock(IlluminaSequencingRunFactory.class);

        IlluminaFlowcellDao flowcellDao = EasyMock.createNiceMock(IlluminaFlowcellDao.class);
        EasyMock.expect(flowcellDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(flowcell);

        LabVesselDao vesselDao = EasyMock.createNiceMock(LabVesselDao.class);
        VesselTransferEjb vesselTransferEjb = EasyMock.createMock(VesselTransferEjb.class);
        BSPExportsService bspExportsService = EasyMock.createMock(BSPExportsService.class);

        SystemRouter router = new SystemRouter(vesselDao, EasyMock.createNiceMock(ControlDao.class),
                new WorkflowLoader(),
                EasyMock.createNiceMock(BSPSampleDataFetcher.class), bspExportsService);
        HipChatMessageSender hipChatMsgSender = EasyMock.createNiceMock(HipChatMessageSender.class);
        MiSeqReagentKitDao reagentKitDao = EasyMock.createNiceMock(MiSeqReagentKitDao.class);

        SolexaRunResource runResource =
                new SolexaRunResource(runDao, runFactory, flowcellDao, vesselTransferEjb, router,
                        SquidConnectorProducer.stubInstance(), hipChatMsgSender, SquidConfig.produce(Deployment.STUBBY),
                        reagentKitDao);

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(runDao, runFactory, flowcellDao, vesselDao, uriInfoMock, hipChatMsgSender);

        javax.ws.rs.core.Response response = runResource.createRun(runBean, uriInfoMock);
        Assert.assertEquals(SolexaRunBean.class, response.getEntity().getClass());
        EasyMock.verify(runDao, flowcellDao, vesselDao, runFactory);
    }

    @Test(groups = TestGroups.DATABASE_FREE, enabled = false)
    public void testExomeExpressFlowcell() throws Exception {

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        Date runDate = new Date();

        IlluminaSequencingRunDao runDao = EasyMock.createMock(IlluminaSequencingRunDao.class);

        EasyMock.expect(runDao.findByRunName(EasyMock.anyObject(String.class))).andReturn(null).times(2);
        IlluminaSequencingRunFactory runFactory = EasyMock.createMock(IlluminaSequencingRunFactory.class);

        LabVesselDao vesselDao = EasyMock.createNiceMock(LabVesselDao.class);
        BSPExportsService bspExportsService = EasyMock.createMock(BSPExportsService.class);

        SystemRouter router = new SystemRouter(vesselDao, EasyMock.createNiceMock(ControlDao.class),
                new WorkflowLoader(),
                EasyMock.createNiceMock(BSPSampleDataFetcher.class), bspExportsService);
        HipChatMessageSender hipChatMsgSender = EasyMock.createNiceMock(HipChatMessageSender.class);
        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);


        final ProductOrder productOrder =
                ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");

        LabBatch workflowBatch = new LabBatch("Whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.WHOLE_GENOME);

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PreFlightEntityBuilder preFlightEntityBuilder = runPreflightProcess(mapBarcodeToTube, "1");
        ShearingEntityBuilder shearingEntityBuilder =
                runShearingProcess(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                        preFlightEntityBuilder.getRackBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(shearingEntityBuilder.getShearingCleanupPlate(),
                        shearingEntityBuilder.getShearCleanPlateBarcode(), shearingEntityBuilder.getShearingPlate(),
                        "1");
        SageEntityBuilder sageEntityBuilder = runSageProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes());

        Assert.assertEquals(sageEntityBuilder.getSageCleanupRack().getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder =
                runQtpProcess(sageEntityBuilder.getSageCleanupRack(), sageEntityBuilder.getSageCleanupTubeBarcodes(),
                        sageEntityBuilder.getMapBarcodeToSageUnloadTubes(), "1");

        MiSeqReagentKitEntityBuilder miseqReagentBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1", reagentBlockBarcode);

        VesselTransferEjb vesselTransferEjb = EasyMock.createMock(VesselTransferEjb.class);
        MiSeqReagentKitDao reagentKitDao = EasyMock.createNiceMock(MiSeqReagentKitDao.class);
        EasyMock.expect(reagentKitDao.findByBarcode(EasyMock.anyObject(String.class)))
                .andReturn(miseqReagentBuilder.getReagentKit());

        IlluminaFlowcellDao miseqFlowcellDao = EasyMock.createNiceMock(IlluminaFlowcellDao.class);
        EasyMock.expect(miseqFlowcellDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(null);

        final String miseqFlowcellBarcode = "A143C";
        SolexaRunBean miseqRunBean =
                new SolexaRunBean(miseqFlowcellBarcode,
                        miseqFlowcellBarcode + dateFormat.format(runDate),
                        runDate, "Superman",
                        File.createTempFile("tempMiSeqRun" + dateFormat.format(runDate), ".txt")
                                .getAbsolutePath(), "reagentBlk" + runDate.getTime());

        SolexaRunResource miseqRunResource =
                new SolexaRunResource(runDao, runFactory, miseqFlowcellDao, vesselTransferEjb, router,
                        SquidConnectorProducer.failureStubInstance(), hipChatMsgSender,
                        SquidConfig.produce(Deployment.STUBBY),
                        reagentKitDao);


        javax.ws.rs.core.Response miseqResponse = miseqRunResource.createRun(miseqRunBean, uriInfoMock);

        Assert.assertEquals(miseqResponse.getStatus(), Response.Status.OK);


        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", null,
                        ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, "designation", Workflow.WHOLE_GENOME);

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        @SuppressWarnings("UnusedDeclaration")
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();

        IlluminaFlowcell flowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();
        SolexaRunBean runBean =
                new SolexaRunBean(flowcell.getCartridgeBarcode(),
                        flowcell.getCartridgeBarcode() + dateFormat.format(runDate),
                        runDate, "Superman",
                        File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt")
                                .getAbsolutePath(), null);

        IlluminaFlowcellDao flowcellDao = EasyMock.createNiceMock(IlluminaFlowcellDao.class);
        EasyMock.expect(flowcellDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(flowcell);

        MiSeqReagentKitDao placeHolderReagentKitDao = EasyMock.createNiceMock(MiSeqReagentKitDao.class);
        SolexaRunResource runResource =
                new SolexaRunResource(runDao, runFactory, flowcellDao, vesselTransferEjb, router,
                        SquidConnectorProducer.stubInstance(), hipChatMsgSender, SquidConfig.produce(Deployment.STUBBY),
                        placeHolderReagentKitDao);

        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(runDao, runFactory, flowcellDao, vesselDao, uriInfoMock, hipChatMsgSender, reagentKitDao);

        javax.ws.rs.core.Response response = runResource.createRun(runBean, uriInfoMock);
        Assert.assertEquals(SolexaRunBean.class, response.getEntity().getClass());
        EasyMock.verify(runDao, flowcellDao, vesselDao, runFactory, reagentKitDao);
    }

    public void testNoChainOfCustodyRegistration() throws Exception {

        Date runDate = new Date();

        final String flowcellBarcode = "FlowCellBarcode" + runDate.getTime();
        IlluminaFlowcell flowcell = new IlluminaFlowcell(IlluminaFlowcell.FlowcellType.HiSeqFlowcell, flowcellBarcode);
        SolexaRunBean runBean =
                new SolexaRunBean(flowcellBarcode,
                        flowcellBarcode + dateFormat.format(runDate),
                        runDate, "Superman",
                        File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt")
                                .getAbsolutePath(), null);

        IlluminaSequencingRunDao runDao = EasyMock.createMock(IlluminaSequencingRunDao.class);

        EasyMock.expect(runDao.findByRunName(EasyMock.anyObject(String.class))).andReturn(null);

        IlluminaFlowcellDao flowcellDao = EasyMock.createNiceMock(IlluminaFlowcellDao.class);
        EasyMock.expect(flowcellDao.findByBarcode(EasyMock.anyObject(String.class))).andReturn(flowcell);
        LabVesselDao vesselDao = EasyMock.createNiceMock(LabVesselDao.class);

        IlluminaSequencingRunFactory runFactory = EasyMock.createMock(IlluminaSequencingRunFactory.class);
        BSPExportsService bspExportsService = EasyMock.createMock(BSPExportsService.class);
        SystemRouter router = new SystemRouter(vesselDao, EasyMock.createNiceMock(ControlDao.class),
                new WorkflowLoader(),
                EasyMock.createNiceMock(BSPSampleDataFetcher.class), bspExportsService);

        HipChatMessageSender hipChatMsgSender = EasyMock.createNiceMock(HipChatMessageSender.class);
        VesselTransferEjb vesselTransferEjb = EasyMock.createMock(VesselTransferEjb.class);
        MiSeqReagentKitDao reagentKitDao = EasyMock.createNiceMock(MiSeqReagentKitDao.class);

        SolexaRunResource runResource = new SolexaRunResource(runDao, runFactory, flowcellDao, vesselTransferEjb,
                router, SquidConnectorProducer.stubInstance(), hipChatMsgSender, SquidConfig.produce(Deployment.STUBBY),
                reagentKitDao);

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(runDao, runFactory, flowcellDao, vesselDao, uriInfoMock, hipChatMsgSender);

        javax.ws.rs.core.Response response = runResource.createRun(runBean, uriInfoMock);
        Assert.assertEquals(SolexaRunBean.class, response.getEntity().getClass());
        EasyMock.verify(runDao, flowcellDao, vesselDao, runFactory);
    }

    /*
     * For the Happy path of routing logic (that it should call Mercury) see End to End tests in LabEventTest
     */
}
