package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.monitoring.HipChatMessageSender;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnectorProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.*;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 3/14/13
 *         Time: 12:08 AM
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class SolexaRunRoutingTest extends BaseEventTest{

    private static final int NUM_POSITIONS_IN_RACK = 96;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

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

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        final ProductOrder productOrder =
                ProductOrderTestFactory.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        productOrder.getResearchProject().setJiraTicketKey("RP-123");

        AthenaClientServiceStub.addProductOrder(productOrder);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder);

        LabBatch workflowBatch = new LabBatch("Whole Genome Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);

        PreFlightEntityBuilder preFlightEntityBuilder = runPreflightProcess(mapBarcodeToTube, productOrder, workflowBatch);
        ShearingEntityBuilder shearingEntityBuilder = runShearingProcess(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                preFlightEntityBuilder.getRackBarcode());
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(shearingEntityBuilder.getShearingCleanupPlate(),
                shearingEntityBuilder.getShearCleanPlateBarcode(), shearingEntityBuilder.getShearingPlate());
        SageEntityBuilder sageEntityBuilder = runSageProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                libraryConstructionEntityBuilder.getPondRegRackBarcode(), libraryConstructionEntityBuilder.getPondRegTubeBarcodes());

        Assert.assertEquals(sageEntityBuilder.getSageCleanupRack().getSampleInstances().size(), NUM_POSITIONS_IN_RACK, "Wrong number of sage cleanup samples");

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(sageEntityBuilder.getSageCleanupRack(), sageEntityBuilder.getSageCleanupTubeBarcodes(),
                sageEntityBuilder.getMapBarcodeToSageUnloadTubes(), WorkflowName.WHOLE_GENOME);

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                TransferTraverserCriteria.TraversalDirection.Descendants);
        @SuppressWarnings("UnusedDeclaration")
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();

        IlluminaFlowcell flowcell = qtpEntityBuilder.getIlluminaFlowcell();
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

        MercuryOrSquidRouter router = new MercuryOrSquidRouter(vesselDao, AthenaClientProducer.stubInstance());
        HipChatMessageSender hipChatMsgSender = EasyMock.createNiceMock(HipChatMessageSender.class);

        SolexaRunResource runResource = new SolexaRunResource(runDao, runFactory, flowcellDao, router,
                                                                     SquidConnectorProducer.stubInstance(),
                                                                     hipChatMsgSender);

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(uriInfoMock, hipChatMsgSender, vesselDao);

        javax.ws.rs.core.Response response = runResource.createRun(runBean, uriInfoMock);
        Assert.assertEquals(SolexaRunBean.class, response.getEntity().getClass());
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

        IlluminaSequencingRunFactory runFactory = new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));
        MercuryOrSquidRouter router = new MercuryOrSquidRouter(vesselDao, AthenaClientProducer.stubInstance());

        HipChatMessageSender hipChatMsgSender = EasyMock.createNiceMock(HipChatMessageSender.class);

        SolexaRunResource runResource = new SolexaRunResource(runDao, runFactory, flowcellDao, router,
                SquidConnectorProducer.stubInstance(),
                hipChatMsgSender);

        UriInfo uriInfoMock = EasyMock.createNiceMock(UriInfo.class);
        EasyMock.expect(uriInfoMock.getAbsolutePathBuilder()).andReturn(UriBuilder.fromPath(""));
        EasyMock.replay(uriInfoMock, hipChatMsgSender, vesselDao);

        javax.ws.rs.core.Response response = runResource.createRun(runBean, uriInfoMock);
        Assert.assertEquals(SolexaRunBean.class, response.getEntity().getClass());
    }

    /*
     * For the Happy path of routing logic (that it should call Mercury) see End to End tests in LabEventTest
     */
}
