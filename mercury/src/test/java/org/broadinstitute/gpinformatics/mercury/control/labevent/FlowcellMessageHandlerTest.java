package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.MiSeqReagentKitEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests the FlowcellMessageHandler by running through a complete workflow cycle down to the pool test and flowcell
 * transfers
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class FlowcellMessageHandlerTest extends BaseEventTest {

    public static final String TST_REAGENT_KT = "tstReagentKT";
    public static final String MISEQ_TICKET_KEY = "FCT-1";
    public static final String FLOWCELL_2500_TICKET_KEY = "FCT-2";
    private LabVessel denatureSource;
    private Date runDate;
    private QtpEntityBuilder qtpEntityBuilder;
    private TwoDBarcodedTube denatureTube;
    private Set<LabVessel> starterVessels;

    @Override
    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        super.setUp();
        expectedRouting = SystemRouter.System.MERCURY;

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Long pdoId = 9202938094820L;
        AthenaClientServiceStub.addProductOrder(productOrder);
        runDate = new Date();
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());
        workflowBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), "LCSET-tst123"));

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), Workflow.AGILENT_EXOME_EXPRESS, "1");

        denatureTube = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        starterVessels = Collections.singleton((LabVessel) denatureTube);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testSuccessfulDenatureToFlowcellMsg() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch miseqBatch = new LabBatch(MISEQ_TICKET_KEY, starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET_KEY, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal
                .valueOf(12.33f));

        final String denatureToFlowcellFlowcellBarcode = "ADTF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);


        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        // Verify that the tube used to load the flowcell is the denature tube;
        IlluminaFlowcell flowcell = (IlluminaFlowcell) reagentToFlowcellEvent.getTargetLabVessels().iterator().next();
        for (LabVessel tube : flowcell.getNearestTubeAncestorsForLanes().values()) {
            Assert.assertEquals(tube.getLabel(), denatureTube.getLabel());
        }

        Mockito.verify(mockEmailSender, Mockito.never()).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(1)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(2)).updateIssue(Mockito.anyString(), Mockito.anyCollection());


        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        Mockito.verify(mockEmailSender, Mockito.never())
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(2)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(4)).updateIssue(Mockito.anyString(), Mockito.anyCollection());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFailureDenatureToFlowcellMsgNoMiseqBatches() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET_KEY, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(12.33f));

        final String denatureToFlowcellFlowcellBarcode = "ADTF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);


        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        Mockito.verify(mockEmailSender, Mockito.times(1)).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.times(1)).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.never()).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.never()).updateIssue(Mockito.anyString(), Mockito.anyCollection());


        EmailSender mockEmailSender2 = Mockito.mock(EmailSender.class);
        JiraService mockJiraService2 = Mockito.mock(JiraService.class);
        JiraService mockJiraSource2 = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService2.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource2.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig2 = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig2);


        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        Mockito.verify(mockEmailSender2, Mockito.never())
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig2, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService2, Mockito.times(1)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService2, Mockito.times(2)).updateIssue(Mockito.anyString(), Mockito.anyCollection());

    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFailureDenatureToFlowcellMsgTooMany2500Batches() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET_KEY, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(12.33f));
        LabBatch fctBatch2 =
                new LabBatch(FLOWCELL_2500_TICKET_KEY + "2", starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(12.33f));

        final String denatureToFlowcellFlowcellBarcode = "ADTF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);

        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        Mockito.verify(mockEmailSender, Mockito.times(1)).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.times(1)).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.never()).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.never()).updateIssue(Mockito.anyString(), Mockito.anyCollection());

        EmailSender mockEmailSender2 = Mockito.mock(EmailSender.class);
        JiraService mockJiraService2 = Mockito.mock(JiraService.class);
        JiraService mockJiraSource2 = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService2.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource2.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig2 = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig2);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        Mockito.verify(mockEmailSender2, Mockito.times(1))
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig2, Mockito.times(1)).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService2, Mockito.never()).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService2, Mockito.never()).updateIssue(Mockito.anyString(), Mockito.anyCollection());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFailureDenatureToFlowcellMsgNo2500Batches() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch miseqBatch = new LabBatch(MISEQ_TICKET_KEY, starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));

        final String denatureToFlowcellFlowcellBarcode = "ADTF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);

        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        Mockito.verify(mockEmailSender, Mockito.never()).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(1)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(2)).updateIssue(Mockito.anyString(), Mockito.anyCollection());

        EmailSender mockEmailSender2 = Mockito.mock(EmailSender.class);
        JiraService mockJiraService2 = Mockito.mock(JiraService.class);
        JiraService mockJiraSource2 = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService2.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource2.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig2 = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig2);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        Mockito.verify(mockEmailSender2, Mockito.times(1))
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig2, Mockito.times(1)).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService2, Mockito.never()).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService2, Mockito.never()).updateIssue(Mockito.anyString(), Mockito.anyCollection());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testFailureDenatureToFlowcellMsgTooManyMiSeqBatches() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch miseqBatch = new LabBatch(MISEQ_TICKET_KEY, starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));
        LabBatch miseqBatch2 = new LabBatch(MISEQ_TICKET_KEY + "2", starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));

        final String denatureToFlowcellFlowcellBarcode = "ADTF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);

        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        Mockito.verify(mockEmailSender, Mockito.times(1)).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.times(1)).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.never()).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.never()).updateIssue(Mockito.anyString(), Mockito.anyCollection());

        EmailSender mockEmailSender2 = Mockito.mock(EmailSender.class);
        JiraService mockJiraService2 = Mockito.mock(JiraService.class);
        JiraService mockJiraSource2 = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService2.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource2.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig2 = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService2);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig2);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.DENATURE_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        Mockito.verify(mockEmailSender2, Mockito.times(1))
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig2, Mockito.times(1)).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService2, Mockito.never()).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService2, Mockito.never()).updateIssue(Mockito.anyString(), Mockito.anyCollection());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testSuccessfulDenatureToDilutionMsg() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch miseqBatch = new LabBatch(MISEQ_TICKET_KEY, starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET_KEY, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(12.33f));
        LabBatch fctBatch2 =
                new LabBatch(FLOWCELL_2500_TICKET_KEY + "2", starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(12.33f));

        final String denatureToFlowcellFlowcellBarcode = "ADDF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);

        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        Mockito.verify(mockEmailSender, Mockito.never()).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(1)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(2)).updateIssue(Mockito.anyString(), Mockito.anyCollection());

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);

        Mockito.verify(mockEmailSender, Mockito.never())
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(2)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(4)).updateIssue(Mockito.anyString(), Mockito.anyCollection());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testSuccessfulSTBToFlowcellMsg() throws Exception {

        //create a couple Miseq batches then one FCT (2500) batch
        LabBatch miseqBatch = new LabBatch(MISEQ_TICKET_KEY, starterVessels, LabBatch.LabBatchType.MISEQ, BigDecimal.valueOf(7f));
        LabBatch fctBatch = new LabBatch(FLOWCELL_2500_TICKET_KEY, starterVessels, LabBatch.LabBatchType.FCT, BigDecimal.valueOf(12.33f));

        final String denatureToFlowcellFlowcellBarcode = "ASTF";

        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        JiraService mockJiraService = Mockito.mock(JiraService.class);
        JiraService mockJiraSource = JiraServiceProducer.stubInstance();
        Mockito.when(mockJiraService.getCustomFields(Mockito.eq(LabBatch.TicketFields.SUMMARY.getName()),
                Mockito.eq(LabBatch.TicketFields.SEQUENCING_STATION.getName())))
                .thenReturn(mockJiraSource.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                        LabBatch.TicketFields.SEQUENCING_STATION.getName()));
        AppConfig mockAppConfig = Mockito.mock(AppConfig.class);

        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setEmailSender(mockEmailSender);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setJiraService(mockJiraService);
        getLabEventFactory().getEventHandlerSelector().getFlowcellMessageHandler().setAppConfig(mockAppConfig);

        TubeFormationDao mockTubeFormationDao = Mockito.mock(TubeFormationDao.class);
        Mockito.when(mockTubeFormationDao.findByDigest(Mockito.anyString()))
                .thenReturn(qtpEntityBuilder.getDenatureRack());
        getLabEventFactory().setTubeFormationDao(mockTubeFormationDao);

        TwoDBarcodedTubeDao mockTwoDTubeDao = Mockito.mock(TwoDBarcodedTubeDao.class);
        Mockito.when(mockTwoDTubeDao.findByBarcodes(Mockito.anyList()))
                .thenReturn(new HashMap<String, TwoDBarcodedTube>() {{
                    put(denatureTube.getLabel(), denatureTube);
                }});
        getLabEventFactory().setTwoDBarcodedTubeDao(mockTwoDTubeDao);

        RackOfTubesDao mockRackOfTubes = Mockito.mock(RackOfTubesDao.class);
        Mockito.when(mockRackOfTubes.findByBarcode(Mockito.anyString())).thenReturn(null);
        getLabEventFactory().setRackOfTubesDao(mockRackOfTubes);

        MiSeqReagentKitEntityBuilder miSeqReagentKitEntityBuilder =
                runMiSeqReagentEntityBuilder(qtpEntityBuilder.getDenatureRack(), "1",
                        TST_REAGENT_KT).invoke();

        Map<String, LabVessel> mockedMap = new HashMap<>();
        mockedMap.put(miSeqReagentKitEntityBuilder.getReagentKit().getLabel(),
                miSeqReagentKitEntityBuilder.getReagentKit());
        mockedMap.put(denatureToFlowcellFlowcellBarcode, null);

        LabVesselDao mockLabVesselDao2 = Mockito.mock(LabVesselDao.class);
        Mockito.when(mockLabVesselDao2.findByBarcodes(Mockito.anyList())).thenReturn(mockedMap);

        getLabEventFactory().setLabVesselDao(mockLabVesselDao2);

        PlateCherryPickEvent reagentToFlowcellJaxb =
                getLabEventFactory()
                        .getReagentToFlowcellEventDBFree(TST_REAGENT_KT, denatureToFlowcellFlowcellBarcode, "hrafal",
                                "ZAN");
        LabEvent reagentToFlowcellEvent = getLabEventFactory().buildFromBettaLims(reagentToFlowcellJaxb);
        getLabEventFactory().getEventHandlerSelector()
                .applyEventSpecificHandling(reagentToFlowcellEvent, reagentToFlowcellJaxb);

        Mockito.verify(mockEmailSender, Mockito.never()).sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(1)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(2)).updateIssue(Mockito.anyString(), Mockito.anyCollection());

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FLOWCELL_2500_TICKET_KEY,
                        ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL, null,
                        Workflow.AGILENT_EXOME_EXPRESS);
        Mockito.verify(mockEmailSender, Mockito.never())
                .sendHtmlEmail((AppConfig)Mockito.anyObject(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockAppConfig, Mockito.never()).getWorkflowValidationEmail();
        Mockito.verify(mockJiraService, Mockito.times(2)).getCustomFields(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockJiraService, Mockito.times(4)).updateIssue(Mockito.anyString(), Mockito.anyCollection());
    }
}
