package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

//import com.jprofiler.api.agent.Controller;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcherStub;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationSetupEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResource;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowValidator;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingJaxbBuilder;
import org.easymock.EasyMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test the web service
 */
@SuppressWarnings("OverlyCoupledClass")
@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class BettaLimsMessageResourceTest extends Arquillian {

    public BettaLimsMessageResourceTest(){}

    public static final String PICO_PLATING_BUCKET = "Pico/Plating Bucket";
    public static final String ICE_BUCKET = "ICE Bucket";

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private SolexaRunResource solexaRunResource;

    @Inject
    private BarcodedTubeDao barcodedTubeDao;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private IlluminaRunResource illuminaRunResource;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private BucketEjb bucketEjb;

    @Inject
    private JiraService jiraService;

    @Inject
    private ProductOrderJiraUtil productOrderJiraUtil;

    @Inject
    private WorkflowValidator workflowValidator;

    @Inject
    private AppConfig appConfig;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private BucketDao bucketDao;

    private final SimpleDateFormat testPrefixDateFormat = new SimpleDateFormat("MMddHHmmss");

    public static final Map<String, String> mapWorkflowToPartNum = new HashMap<String, String>() {{
        put(Workflow.WHOLE_GENOME, "P-WG-0002");
        put(Workflow.AGILENT_EXOME_EXPRESS, "P-EX-0002");
        put(Workflow.ICE_EXOME_EXPRESS, "P-EX-0012");
        put(Workflow.ICE_CRSP, "P-CLA-0004");
        put(Workflow.HYBRID_SELECTION, "P-EX-0001");
    }};


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder
                .buildMercuryWarWithAlternatives(DEV, SampleDataFetcherStub.EverythingYouAskForYouGetAndItsHuman.class);
    }

    @Test
    public void testNonExistingSourceTube() {
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(false);
        String testPrefix = testPrefixDateFormat.format(new Date());
        PlateTransferEventType transfer = bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetup",
                testPrefix + "R", Collections.singletonList(testPrefix + "T"), testPrefix + "P");
        boolean exception = false;
        try {
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(transfer);
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to find tube"));
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testNonExistingSourcePlate() {
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(false);
        String testPrefix = testPrefixDateFormat.format(new Date());

        PlateTransferEventType transfer = bettaLimsMessageTestFactory.buildPlateToPlate("FingerprintingPlateSetup",
                testPrefix + "P1", testPrefix + "P2");
        boolean exception = false;
        try {
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add(transfer);
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Failed to find plate"));
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    /**
     * Sends messages for one PDO, then reworks two of those samples along with a second PDO.
     */
    @Test(enabled = false)
    public void testRework() throws ValidationException {
        // Set up one PDO / bucket / batch
        String testPrefix = testPrefixDateFormat.format(new Date());
        ProductOrder productOrder1 = buildProductOrder(testPrefix, BaseEventTest.NUM_POSITIONS_IN_RACK,
                Workflow.AGILENT_EXOME_EXPRESS);
        Map<String, BarcodedTube> mapBarcodeToTube = buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, barcodedTubeDao, MercurySample.MetadataSource.BSP, productOrder1);
        bucketAndBatch(testPrefix, productOrder1, mapBarcodeToTube);
        // message
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(false);
        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, barcodedTubeDao, appConfig.getUrl(), BaseEventTest.NUM_POSITIONS_IN_RACK);

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1", ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        // Create second PDO
        testPrefix = testPrefixDateFormat.format(new Date());
        ProductOrder productOrder2 = buildProductOrder(testPrefix, BaseEventTest.NUM_POSITIONS_IN_RACK - 2,
                Workflow.AGILENT_EXOME_EXPRESS);
        Map<String, BarcodedTube> mapBarcodeToTube2 = buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK - 2, barcodedTubeDao, MercurySample.MetadataSource.BSP,
                productOrder2);

        // Add two samples from first PDO to bucket
        Set<LabVessel> reworks = new HashSet<>();
        Iterator<Map.Entry<String, BarcodedTube>> iterator = mapBarcodeToTube.entrySet().iterator();
        Map.Entry<String, BarcodedTube> barcodeTubeEntry = iterator.next();
        reworkEjb.addAndValidateCandidates(
                Collections.singleton(
                        new ReworkEjb.BucketCandidate(barcodeTubeEntry.getValue().getLabel(), productOrder1)),
                ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue(), "Test", "jowalsh", PICO_PLATING_BUCKET);
        mapBarcodeToTube2.put(barcodeTubeEntry.getKey(), barcodeTubeEntry.getValue());
        reworks.add(barcodeTubeEntry.getValue());

        barcodeTubeEntry = iterator.next();
        reworkEjb.addAndValidateCandidates(
                Collections.singleton(
                        new ReworkEjb.BucketCandidate(barcodeTubeEntry.getValue().getLabel(), productOrder1)),
                ReworkEntry.ReworkReasonEnum.UNKNOWN_ERROR.getValue(), "Test", "jowalsh", PICO_PLATING_BUCKET);
        mapBarcodeToTube2.put(barcodeTubeEntry.getKey(), barcodeTubeEntry.getValue());
        reworks.add(barcodeTubeEntry.getValue());

        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube2.values());
        bucketEjb.addSamplesToBucket(productOrder2);

        // Create batch
        String batchName = "LCSET-MsgTest-" + testPrefix;
        LabBatch labBatch = new LabBatch(batchName, starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.addReworks(reworks);
        labBatch.setJiraTicket(new JiraTicket(JiraServiceTestProducer.stubInstance(), batchName));

        List<Long> bucketIds = new ArrayList<>();
        List<Long> reworkBucketIds = new ArrayList<>();

        for (LabVessel starter : starters) {
            bucketIds.add(starter.getBucketEntries().iterator().next().getBucketEntryId());
        }

        for (LabVessel rework : reworks) {
            reworkBucketIds.add(rework.getBucketEntries().iterator().next().getBucketEntryId());
        }

        labBatchEjb.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.AGILENT_EXOME_EXPRESS, bucketIds, reworkBucketIds, batchName, "", new Date(),
                "", "jowalsh", PICO_PLATING_BUCKET);
        // message
        hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix, mapBarcodeToTube2, bettaLimsMessageFactory,
                Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, barcodedTubeDao, appConfig.getUrl(), BaseEventTest.NUM_POSITIONS_IN_RACK);

        qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1", ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

    }

    /**
     * Message one LCSET, and register run.
     */
    @Test(enabled = true)
    public void testProcessMessage() throws ValidationException {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<String, BarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, Workflow.AGILENT_EXOME_EXPRESS);

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(false);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, barcodedTubeDao, appConfig.getUrl(), BaseEventTest.NUM_POSITIONS_IN_RACK);

        StaticPlate staticPlate = staticPlateDao.findByBarcode(
                hybridSelectionJaxbBuilder.getGsWash1Jaxb().getPlate().getBarcode());
        List<WorkflowValidator.WorkflowValidationError> validationErrors = workflowValidator.validateWorkflow(
                Collections.<LabVessel>singleton(staticPlate), "EndRepair");
        String renderTemplate = workflowValidator.renderTemplate(staticPlate.getLabel(),
                Collections.singleton(hybridSelectionJaxbBuilder.getGsWash1Jaxb().getEventType()),
                hybridSelectionJaxbBuilder.getGsWash1Jaxb().getStart(),
                hybridSelectionJaxbBuilder.getGsWash1Jaxb().getOperator(),
                hybridSelectionJaxbBuilder.getGsWash1Jaxb().getStation(), validationErrors);
        Assert.assertTrue(renderTemplate.contains("has failed validation"));

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1", ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }
        BarcodedTube poolTube = barcodedTubeDao.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcodes().get(0));
        Assert.assertEquals(poolTube.getSampleInstancesV2().size(), BaseEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        IlluminaSequencingRun illuminaSequencingRun = registerIlluminaSequencingRun(testPrefix,
                hiSeq2500JaxbBuilder.getFlowcellBarcode());

        ZimsIlluminaRun zimsIlluminaRun = illuminaRunResource.getRun(illuminaSequencingRun.getRunName());
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = zimsIlluminaRun.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), BaseEventTest.NUM_POSITIONS_IN_RACK);
        for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
            Assert.assertEquals(libraryBean.getLcSet(), mapBarcodeToTube.values().iterator().next().
                    getBucketEntries().iterator().next().getLabBatch().getBatchName());
        }

        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            for (BucketEntry entry : tube.getBucketEntries()) {
                entry.setStatus(BucketEntry.Status.Archived);
            }
        }

//        Controller.stopCPURecording();
    }

    /**
     * Message one LCSET, and register run.
     */
    @Test(enabled = true)
    public void testIce() throws ValidationException {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<String, BarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, Workflow.ICE_EXOME_EXPRESS);

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(false);

        String shearingRackBarcode = "ShearRack" + testPrefix;
        ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, shearingRackBarcode).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageFactory, testPrefix, shearingJaxbBuilder.getShearCleanPlateBarcode(),
                LibraryConstructionJaxbBuilder.P_7_INDEX_PLATE_BARCODE,
                LibraryConstructionJaxbBuilder.P_5_INDEX_PLATE_BARCODE, BaseEventTest.NUM_POSITIONS_IN_RACK,
                LibraryConstructionJaxbBuilder.TargetSystem.MERCURY_ONLY,
                Arrays.asList(Triple.of("KAPA Reagent Box", "0009753252", 1)),
                Arrays.asList(Triple.of("PEG", "0009753352", 2), Triple.of("70% Ethanol", "LCEtohTest", 3),
                        Triple.of("EB", "0009753452", 4), Triple.of("SPRI", "LCSpriTest", 5)),
                Arrays.asList(Triple.of("KAPA Amp Kit", "0009753250", 6)),
                LibraryConstructionJaxbBuilder.PondType.REGULAR).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        IceJaxbBuilder iceJaxbBuilder = new IceJaxbBuilder(bettaLimsMessageFactory,
                testPrefix, Collections.singletonList(libraryConstructionJaxbBuilder.getPondRegRackBarcode()),
                Collections.singletonList(libraryConstructionJaxbBuilder.getPondRegTubeBarcodes()),
                "Bait" + testPrefix, "Bait" + testPrefix,
                LibraryConstructionJaxbBuilder.TargetSystem.MERCURY_ONLY, IceJaxbBuilder.PlexType.PLEX96,
                Arrays.asList(Triple.of("CT3", "0009763452", 1),
                        Triple.of("Rapid Capture Kit bait", "0009773452", 2),
                        Triple.of("Rapid Capture Kit Resuspension Buffer", "0009783452", 3)),
                Arrays.asList(Triple.of("Dual Index Primers Lot", "0009764452", 4),
                        Triple.of("Rapid Capture Enrichment Amp Lot Barcode", "0009765452", 5))
        ).invoke();
        List<ReagentDesign> reagentDesigns = reagentDesignDao.findAll(ReagentDesign.class, 0, 1);
        ReagentDesign baitDesign = null;
        if (reagentDesigns != null && !reagentDesigns.isEmpty()) {
            baitDesign = reagentDesigns.get(0);
        }

        barcodedTubeDao.persist(LabEventTest.buildBaitTube(iceJaxbBuilder.getBaitTube1Barcode(),
                baitDesign));

        for (BettaLIMSMessage bettaLIMSMessage : iceJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(iceJaxbBuilder.getCatchEnrichTubeBarcodes()),
                Collections.singletonList(iceJaxbBuilder.getCatchEnrichRackBarcode()),
                true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1", ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }
        BarcodedTube poolTube = barcodedTubeDao.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcodes().get(0));
        Assert.assertEquals(poolTube.getSampleInstancesV2().size(), BaseEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        IlluminaSequencingRun illuminaSequencingRun = registerIlluminaSequencingRun(testPrefix,
                hiSeq2500JaxbBuilder.getFlowcellBarcode());

        ZimsIlluminaRun zimsIlluminaRun = illuminaRunResource.getRun(illuminaSequencingRun.getRunName());
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
        ZimsIlluminaChamber zimsIlluminaChamber = zimsIlluminaRun.getLanes().iterator().next();
        Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), BaseEventTest.NUM_POSITIONS_IN_RACK);
        for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
            Assert.assertEquals(libraryBean.getLcSet(), mapBarcodeToTube.values().iterator().next().
                    getBucketEntries().iterator().next().getLabBatch().getBatchName());
        }

        for (BarcodedTube tube : mapBarcodeToTube.values()) {
            for (BucketEntry entry : tube.getBucketEntries()) {
                entry.setStatus(BucketEntry.Status.Archived);
            }
        }

//        Controller.stopCPURecording();
    }

    /**
     * Send Mercury a seq plating norm message for tubes/samples that Mercury does not know about.
     * Mercury should still update vol/conc in BSP (GPLIM-3304).
     */
    @Test(enabled = true)
    public void testUpdateBspForNonMercurySamples() {
        String testPrefix = testPrefixDateFormat.format(new Date());
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(false);
        List<BettaLIMSMessage> messages = new ArrayList<>();
        // Uses these tubes from BSP rack CO-9153199
        // SM-1NO68 - 1036273087
        // SM-1NO61 - 0097414383
        // SM-1NWD9 - 1035642036

        String[] barcodes = new String[]{"1036273087", "0097414383", "1035642036"};
        String[] sampleNames = new String[]{"SM-1NO68", "SM-1NO61", "SM-1NWD9"};
        // Verify none of the samples are known to mercury.  It's not really a code failure if for some reason
        // Mercury dev gains awareness of these samples, but the test cannot continue.
        Assert.assertTrue(CollectionUtils.isEmpty(barcodedTubeDao.findListByList(MercurySample.class,
                MercurySample_.sampleKey, Arrays.asList(sampleNames))));
        PlateEventType plateEvent = bettaLimsMessageFactory.buildRackEvent("SeqPlatingNormalization",
                "CO-9153199" + testPrefix, Arrays.asList(barcodes));
        BettaLIMSMessage bettaLIMSMessage = bettaLimsMessageFactory.addMessage(new ArrayList<BettaLIMSMessage>(),
                plateEvent);
        // Override routing to prevent this message from going to Squid, where it could fail.
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        PositionMapType positionMapType = bettaLIMSMessage.getPlateEvent().iterator().next().getPositionMap();
        Assert.assertEquals(positionMapType.getReceptacle().size(), 3);
        // Sets the volume from parts of month, day, hour, minute, second string.
        Assert.assertEquals(testPrefix.length(), 10);
        BigDecimal[] volumes = new BigDecimal[]{
                new BigDecimal("1" + testPrefix.substring(0, 2) + "." + testPrefix.substring(6, 8)),
                new BigDecimal("1" + testPrefix.substring(2, 4) + "." + testPrefix.substring(7, 9)),
                new BigDecimal("1" + testPrefix.substring(4, 6) + "." + testPrefix.substring(8, 10))};
        for (int i = 0; i < volumes.length; ++i) {
            positionMapType.getReceptacle().get(i).setVolume(volumes[i]);
        }
        sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        Map<String, BspSampleData> sampleDataMap = bspSampleDataFetcher.fetchSampleData(Arrays.asList(sampleNames));
        for (int i = 0; i < volumes.length; ++i) {
            // Should have 2 digit accuracy.
            Assert.assertTrue(Math.abs(volumes[i].doubleValue() - sampleDataMap.get(sampleNames[i]).getVolume()) < 0.01,
                    "Expect " + volumes[i].doubleValue() + " Actual " + sampleDataMap.get(sampleNames[i]).getVolume());
        }
    }

    /**
     * Register a flowcell run
     *
     * @param testPrefix      make barcodes unique
     * @param flowcellBarcode flowcell to register
     *
     * @return registered run
     */
    private IlluminaSequencingRun registerIlluminaSequencingRun(String testPrefix, String flowcellBarcode) {
        Date runDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
        String runName = "TestRun" + testPrefix + runDate.getTime() + ".txt";

        String runPath = "/tmp/file/run/path/" + runName;

        SolexaRunBean solexaRunBean = new SolexaRunBean(flowcellBarcode, flowcellBarcode + format.format(runDate),
                runDate, BettaLimsMessageTestFactory.HISEQ_SEQUENCING_STATION_MACHINE_NAME, runPath, null);

        UriInfo uriInfo = EasyMock.createMock(UriInfo.class);
        UriBuilder uriBuilder1 = EasyMock.createMock(UriBuilder.class);
        UriBuilder uriBuilder2 = EasyMock.createMock(UriBuilder.class);

        EasyMock.expect(uriInfo.getAbsolutePathBuilder()).andReturn(uriBuilder1);
        EasyMock.expect(uriBuilder1.path(EasyMock.anyObject(String.class))).andReturn(uriBuilder2);
        EasyMock.expectLastCall().times(2);
        try {
            EasyMock.expect(uriBuilder2.build()).andReturn(new URI("http://xyz"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        EasyMock.expectLastCall().times(2);
        EasyMock.replay(uriBuilder1, uriBuilder2, uriInfo);

        solexaRunResource.createRun(solexaRunBean, uriInfo);
        return illuminaSequencingRunDao.findByRunName(runName);
    }

    /**
     * Send messages for Preflight, Shearing, Library Construction and Hybridization
     *
     * @param bettalimsMessageResource
     * @param reagentDesignDao
     * @param barcodedTubeDao
     * @param testMercuryUrl
     * @param numPositionsInRack
     * @param testPrefix               make barcodes unique
     * @param mapBarcodeToTube         map from tube barcode to sample tube
     * @param bettaLimsMessageFactory  to build messages
     *
     * @return allows access to catch tubes
     */
    public static HybridSelectionJaxbBuilder sendMessagesUptoCatch(String testPrefix,
                                                                   Map<String, BarcodedTube> mapBarcodeToTube,
                                                                   BettaLimsMessageTestFactory bettaLimsMessageFactory,
                                                                   String workflow,
                                                                   BettaLimsMessageResource bettalimsMessageResource,
                                                                   ReagentDesignDao reagentDesignDao,
                                                                   BarcodedTubeDao barcodedTubeDao,
                                                                   String testMercuryUrl, int numPositionsInRack) {

        String shearingRackBarcode;
        if (workflow.equals(Workflow.AGILENT_EXOME_EXPRESS)) {
            shearingRackBarcode = "ShearRack" + testPrefix;
        } else {
            PreFlightJaxbBuilder preFlightJaxbBuilder = new PreFlightJaxbBuilder(
                    bettaLimsMessageFactory, testPrefix, new ArrayList<>(mapBarcodeToTube.keySet())).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : preFlightJaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage, bettalimsMessageResource, testMercuryUrl);
            }
            shearingRackBarcode = preFlightJaxbBuilder.getRackBarcode();
        }
        ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, shearingRackBarcode).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettalimsMessageResource, testMercuryUrl);
        }

        LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageFactory, testPrefix, shearingJaxbBuilder.getShearCleanPlateBarcode(),
                LibraryConstructionJaxbBuilder.P_7_INDEX_PLATE_BARCODE,
                LibraryConstructionJaxbBuilder.P_5_INDEX_PLATE_BARCODE, numPositionsInRack,
                LibraryConstructionJaxbBuilder.TargetSystem.SQUID_VIA_MERCURY,
                Arrays.asList(Triple.of("KAPA Reagent Box", "0009753252", 1)),
                Arrays.asList(Triple.of("PEG", "0009753352", 2),
                        Triple.of("70% Ethanol", "LCEtohTest", 3),
                        Triple.of("EB", "0009753452", 4),
                        Triple.of("SPRI", "LCSpriTest", 5)),
                Arrays.asList(Triple.of("KAPA Amp Kit", "0009753250", 6)),
                LibraryConstructionJaxbBuilder.PondType.REGULAR).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettalimsMessageResource, testMercuryUrl);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(bettaLimsMessageFactory,
                testPrefix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                libraryConstructionJaxbBuilder.getPondRegTubeBarcodes(), "Bait" + testPrefix).invoke();
        List<ReagentDesign> reagentDesigns = reagentDesignDao.findAll(ReagentDesign.class, 0, 1);
        ReagentDesign baitDesign = null;
        if (reagentDesigns != null && !reagentDesigns.isEmpty()) {
            baitDesign = reagentDesigns.get(0);
        }

        barcodedTubeDao.persist(LabEventTest.buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(),
                baitDesign));

        for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
            // BaitSetup is normally routed to BOTH Mercury and Squid, because there isn't enough information in the
            // source or destination to be more accurate.  However, this test doesn't create the source in Squid,
            // so the message would fail there.  To avoid this, override routing.
            if (!bettaLIMSMessage.getReceptaclePlateTransferEvent().isEmpty() &&
                bettaLIMSMessage.getReceptaclePlateTransferEvent().get(0).getEventType().equals("BaitSetup")) {
                bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
            }
            sendMessage(bettaLIMSMessage, bettalimsMessageResource, testMercuryUrl);
        }
        return hybridSelectionJaxbBuilder;
    }

    /**
     * Build samples in plastic, and associate them with a PDO
     *
     * @param testPrefix      make barcodes unique
     * @param numberOfSamples how many samples to create
     *
     * @return map from tube barcode to sample tube
     */
    private Map<String, BarcodedTube> buildSamplesInPdo(String testPrefix, int numberOfSamples,
                                                        String workflow) throws ValidationException {
        ProductOrder productOrder = buildProductOrder(testPrefix, numberOfSamples, workflow);
        Map<String, BarcodedTube> mapBarcodeToTube = buildSampleTubes(testPrefix, numberOfSamples,
                barcodedTubeDao, workflow.equals(Workflow.ICE_CRSP) ? MercurySample.MetadataSource.MERCURY :
                        MercurySample.MetadataSource.BSP, productOrder);
        bucketAndBatch(testPrefix, productOrder, mapBarcodeToTube);
        return mapBarcodeToTube;
    }

    /**
     * Build a product order, including product and research project
     *
     * @param testPrefix      make unique
     * @param numberOfSamples how many samples
     *
     * @return product order
     */
    private ProductOrder buildProductOrder(String testPrefix, int numberOfSamples, String workflow) {
        String partNumber = mapWorkflowToPartNum.get(workflow);
        Product product = productDao.findByPartNumber(partNumber);
        if (product == null) {
            // todo jmt change to exome express
            product = new Product("Standard Exome Sequencing", productFamilyDao.find("Exome"),
                    "Standard Exome Sequencing", "P-EX-0001", new Date(), null, 1814400, 1814400, 184, null, null,
                    null, true, Workflow.HYBRID_SELECTION, false, "agg type");
            product.setPrimaryPriceItem(new PriceItem("1234", PriceItem.PLATFORM_GENOMICS, "Pony Genomics",
                    "Standard Pony"));
            productDao.persist(product);
        }

        String researchProjectKey = workflow.equals(Workflow.ICE_CRSP) ? "RP-926" : "RP-19";
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        if (researchProject == null) {
            researchProject = new ResearchProject(10950L, "SIGMA Sarcoma", "SIGMA Sarcoma", false,
                    ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
            researchProjectDao.persist(researchProject);
        }

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= numberOfSamples; rackPosition++) {
            String bspStock = "SM-" + testPrefix + rackPosition;
            productOrderSamples.add(new ProductOrderSample(bspStock));
        }

        ProductOrder productOrder =
                new ProductOrder(ResearchProjectTestFactory.TEST_CREATOR, "Messaging Test " + testPrefix,
                        productOrderSamples, "GSP-123",
                        product, researchProject);
        productOrder.prepareToSave(bspUserList.getByUsername("QADudePDM"));
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrderDao.persist(productOrder);
        try {
            productOrderJiraUtil.createIssueForOrder(productOrder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return productOrder;
    }

    /**
     * Build samples and tubes
     *
     * @param testPrefix      make unique
     * @param numberOfSamples how many samples
     * @param barcodedTubeDao
     * @param metadataSource
     *
     * @return map from tube barcode to tube
     */
    public static Map<String, BarcodedTube> buildSampleTubes(String testPrefix, int numberOfSamples,
                                                             BarcodedTubeDao barcodedTubeDao,
                                                             MercurySample.MetadataSource metadataSource,
                                                             ProductOrder productOrder) {
        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        Iterator<ProductOrderSample> iterator = productOrder.getSamples().iterator();
        for (int rackPosition = 1; rackPosition <= numberOfSamples; rackPosition++) {
            String barcode = "R" + testPrefix + rackPosition;
            String bspStock = "SM-" + testPrefix + rackPosition;
            BarcodedTube bspAliquot = new BarcodedTube(barcode);
            MercurySample mercurySample = new MercurySample(bspStock, metadataSource);
            mercurySample.addProductOrderSample(iterator.next());
            bspAliquot.addSample(mercurySample);
            mapBarcodeToTube.put(barcode, bspAliquot);

            barcodedTubeDao.persist(bspAliquot);
        }
        return mapBarcodeToTube;
    }

    /**
     * Add a product order's samples to the bucket, and create a batch from the bucket
     *
     * @param testPrefix       make unique
     * @param productOrder     contains sample
     * @param mapBarcodeToTube tubes
     */
    private void bucketAndBatch(String testPrefix, ProductOrder productOrder,
                                Map<String, BarcodedTube> mapBarcodeToTube) throws ValidationException {
        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());
        bucketEjb.addSamplesToBucket(productOrder);

        String batchName = "LCSET-MsgTest-" + testPrefix;
        List<Long> bucketIds = new ArrayList<>();
        String bucketName = null;

        for (LabVessel starter : starters) {
            BucketEntry next = starter.getBucketEntries().iterator().next();
            bucketName = next.getBucket().getBucketDefinitionName();
            bucketIds.add(next.getBucketEntryId());
        }


        labBatchEjb.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.AGILENT_EXOME_EXPRESS, bucketIds, Collections.<Long>emptyList(), batchName,
                "", new Date(), "", "jowalsh", bucketName);

    }

    /**
     * Test performance by creating a flowcell with a different 96 sample LCSET on each of 8 lanes.
     * Needs -Dorg.jboss.remoting-jmx.timeout=3000
     */
    @Test(enabled = false)
    public void test8Lcsets() throws ValidationException {
        String testPrefix;
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(false);
        List<List<String>> listLcsetListNormCatchBarcodes = new ArrayList<>();
        List<String> normCatchRackBarcodes = new ArrayList<>();

        // Get to catch, for 8 LCSETs
        for (int i = 0; i < 8; i++) {
            testPrefix = testPrefixDateFormat.format(new Date());
            Map<String, BarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                    BaseEventTest.NUM_POSITIONS_IN_RACK, Workflow.HYBRID_SELECTION);
            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder =
                    sendMessagesUptoCatch(testPrefix, mapBarcodeToTube, bettaLimsMessageFactory,
                            Workflow.HYBRID_SELECTION, bettaLimsMessageResource,
                            reagentDesignDao, barcodedTubeDao, appConfig.getUrl(),
                            BaseEventTest.NUM_POSITIONS_IN_RACK);
            listLcsetListNormCatchBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchBarcodes());
            normCatchRackBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchRackBarcode());
        }

        // Combine 8 LCSETs on one flowcell
        testPrefix = testPrefixDateFormat.format(new Date());
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(qtpJaxbBuilder.getDenatureTubeBarcode()),
                qtpJaxbBuilder.getDenatureRackBarcode(), null, ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 8);

        IlluminaSequencingRun illuminaSequencingRun =
                registerIlluminaSequencingRun(testPrefix, hiSeq2500JaxbBuilder.getFlowcellBarcode());
        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge().getSampleInstancesV2().size(),
                BaseEventTest.NUM_POSITIONS_IN_RACK * 8, "Wrong number of sample instances");
        ZimsIlluminaRun zimsIlluminaRun = illuminaRunResource.getRun(illuminaSequencingRun.getRunName());
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
    }

    @Test
    public void testActivity() {
        BettaLIMSMessage beginMessage = new BettaLIMSMessage();
        StationSetupEvent beginEvent = new StationSetupEvent();
        beginEvent.setDisambiguator(1L);
        beginEvent.setEventType(LabEventType.ACTIVITY_BEGIN.getName());
        beginEvent.setStart(new Date());
        beginEvent.setStation("BUZZ");
        beginMessage.setStationSetupEvent(beginEvent);
        sendMessage(beginMessage, bettaLimsMessageResource, appConfig.getUrl());
    }

    /**
     * Test that Exome Express LCSETs can be combined with a CRSP LCSET during the ICE process.
     */
    @Test
    public void testSonic() throws ValidationException {
        String date = testPrefixDateFormat.format(new Date());
        int numExExLcsets = 2;
        List<String> exExTestPrefixes = new ArrayList<>();
        List<Pair<LibraryConstructionJaxbBuilder, Map<String, BarcodedTube>>> exExBuilderMapPairs = new ArrayList<>();
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(false);
        List<String> regulatoryDesignations = new ArrayList<>();

        for (int i = 0; i < numExExLcsets; i++) {
            exExTestPrefixes.add(date + "X" + i);
            exExBuilderMapPairs.add(libraryConstruction(exExTestPrefixes.get(i), bettaLimsMessageFactory,
                    Workflow.ICE_EXOME_EXPRESS));
            regulatoryDesignations.add("RESEARCH_ONLY");
        }
        regulatoryDesignations.add("CLINICAL_DIAGNOSTICS");
        String crspTestPrefix = date + "C";

        Pair<LibraryConstructionJaxbBuilder, Map<String, BarcodedTube>> crspBuilderMapPair = libraryConstruction(
                crspTestPrefix, bettaLimsMessageFactory, Workflow.ICE_CRSP);
        BarcodedTube crspTube = crspBuilderMapPair.getRight().values().iterator().next();
        LibraryConstructionJaxbBuilder libraryConstructionCrspJaxbBuilder = crspBuilderMapPair.getLeft();

        List<String> pondRegRackBarcodes = new ArrayList<>();
        List<List<String>> listPondRegTubeBarcodes = new ArrayList<>();
        List<String> batchNames = new ArrayList<>();
        List<Set<String>> listSampleNamesSet = new ArrayList<>();
        for (int i = 0; i < numExExLcsets; i++) {
            pondRegRackBarcodes.add(exExBuilderMapPairs.get(i).getLeft().getPondRegRackBarcode());
            listPondRegTubeBarcodes.add(exExBuilderMapPairs.get(i).getLeft().getPondRegTubeBarcodes());
            Collection<BarcodedTube> barcodedTubes = exExBuilderMapPairs.get(i).getRight().values();
            batchNames.add(barcodedTubes.iterator().next().getLabBatches().iterator().next().getBatchName());
            Set<String> sampleNames = new HashSet<>();
            listSampleNamesSet.add(sampleNames);
            for (BarcodedTube barcodedTube : barcodedTubes) {
                sampleNames.add(barcodedTube.getMercurySamples().iterator().next().getSampleKey());
            }
        }
        pondRegRackBarcodes.add(libraryConstructionCrspJaxbBuilder.getPondRegRackBarcode());
        listPondRegTubeBarcodes.add(libraryConstructionCrspJaxbBuilder.getPondRegTubeBarcodes());
        batchNames.add(crspTube.getLabBatches().iterator().next().getBatchName());
        Set<String> sampleNames = new HashSet<>();
        listSampleNamesSet.add(sampleNames);
        Collection<BarcodedTube> barcodedTubes = crspBuilderMapPair.getRight().values();
        for (BarcodedTube barcodedTube : barcodedTubes) {
            sampleNames.add(barcodedTube.getMercurySamples().iterator().next().getSampleKey());
        }

        List<String> testPrefixes = new ArrayList<>();
        testPrefixes.addAll(exExTestPrefixes);
        testPrefixes.add(crspTestPrefix);

        List<ZimsIlluminaRun> zimsIlluminaRuns = iceAndQtp(testPrefixes, batchNames, bettaLimsMessageFactory,
                pondRegRackBarcodes, listPondRegTubeBarcodes);

        List<LabBatch> exExReworkLabBatches = new ArrayList<>();
        for (int i = 0; i < numExExLcsets; i++) {
            exExReworkLabBatches.add(reworkBatch(exExTestPrefixes.get(i),
                    exExBuilderMapPairs.get(i).getRight().values().iterator().next(),
                    exExBuilderMapPairs.get(i).getLeft().getPondRegTubeBarcodes()));
        }
        LabBatch crspReworkLabBatch = reworkBatch(crspTestPrefix, crspTube,
                libraryConstructionCrspJaxbBuilder.getPondRegTubeBarcodes());

        List<String> reworkTestPrefixes = new ArrayList<>();
        List<String> reworkLabBatchNames = new ArrayList<>();
        for (int i = 0; i < numExExLcsets; i++) {
            reworkTestPrefixes.add(exExTestPrefixes.get(i) + "R");
            reworkLabBatchNames.add(exExReworkLabBatches.get(i).getBatchName());
        }
        reworkTestPrefixes.add(crspTestPrefix + "R");
        reworkLabBatchNames.add(crspReworkLabBatch.getBatchName());
        List<ZimsIlluminaRun> zimsIlluminaReworkRuns = iceAndQtp(reworkTestPrefixes, reworkLabBatchNames,
                bettaLimsMessageFactory, pondRegRackBarcodes, listPondRegTubeBarcodes);

        assertRuns(batchNames, zimsIlluminaRuns, regulatoryDesignations, listSampleNamesSet);

        assertRuns(reworkLabBatchNames, zimsIlluminaReworkRuns, regulatoryDesignations, listSampleNamesSet);
    }

    private void assertRuns(List<String> batchNames, List<ZimsIlluminaRun> zimsIlluminaRuns,
                            List<String> regulatoryDesignations, List<Set<String>> listSampleNamesSet) {
        for (int i = 0; i < zimsIlluminaRuns.size(); i++) {
            ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRuns.get(i);
            Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");
            ZimsIlluminaChamber zimsIlluminaChamber = zimsIlluminaRun.getLanes().iterator().next();
            Assert.assertEquals(zimsIlluminaChamber.getLibraries().size(), BaseEventTest.NUM_POSITIONS_IN_RACK);

            Set<String> sampleIds = new HashSet<>();
            for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
                Assert.assertEquals(libraryBean.getLcSet(), batchNames.get(i));
                sampleIds.add(libraryBean.getSampleId());
                Assert.assertEquals(libraryBean.getRegulatoryDesignation(), regulatoryDesignations.get(i));
            }
            Assert.assertEquals(sampleIds, listSampleNamesSet.get(i));
        }
    }

    @Nonnull
    private LabBatch reworkBatch(String crspTestPrefix, BarcodedTube crspTube, List<String> tubeBarcodes)
            throws ValidationException {
        ProductOrder crspProductOrder = crspTube.getMercurySamples().iterator().next().getProductOrderSamples().
                iterator().next().getProductOrder();
        Map<String, BarcodedTube> mapBarcodeToCrspPond = barcodedTubeDao.findByBarcodes(tubeBarcodes);
        HashSet<LabVessel> crspPonds = new HashSet<LabVessel>(mapBarcodeToCrspPond.values());
        Collection<ReworkEjb.BucketCandidate> bucketCandidates = new HashSet<>();
        for (LabVessel crspPond : crspPonds) {
            bucketCandidates
                    .add(new ReworkEjb.BucketCandidate(crspPond.getLabel(), crspPond.getSampleNames().iterator().next(),
                            crspProductOrder));
        }
         reworkEjb.addAndValidateCandidates(bucketCandidates, "", "comment", "thompson", ICE_BUCKET);

        String batchName = "LCSET-Rework-" + crspTestPrefix;
        List<Long> reworkBucketEntryIds = new ArrayList<>();

        // Have to flush to assign bucketEntryId
        bucketDao.flush();
        for (LabVessel crspPond : crspPonds) {
            reworkBucketEntryIds.add(crspPond.getBucketEntries().iterator().next().getBucketEntryId());
        }

        LabBatch labBatch = labBatchEjb.createLabBatchAndRemoveFromBucket(LabBatch.LabBatchType.WORKFLOW,
                Workflow.ICE_CRSP, Collections.<Long>emptyList(), reworkBucketEntryIds, batchName,
                "", new Date(), "", "thompson", ICE_BUCKET);

        return labBatch;
    }

    private List<ZimsIlluminaRun> iceAndQtp(List<String> testPrefixes, List<String> lcsets,
                                            BettaLimsMessageTestFactory bettaLimsMessageFactory,
                                            List<String> pondRegRackBarcodes,
                                            List<List<String>> listPondRegTubeBarcodes) {
        IceJaxbBuilder iceJaxbBuilder = new IceJaxbBuilder(bettaLimsMessageFactory, testPrefixes.get(0),
                pondRegRackBarcodes, listPondRegTubeBarcodes, "0177198254", "0177198254",
                LibraryConstructionJaxbBuilder.TargetSystem.MERCURY_ONLY, IceJaxbBuilder.PlexType.PLEX96,
                new ArrayList<Triple<String, String, Integer>>() {{
                    add(Triple.of("CT3", "0009763452", 1));
                    add(Triple.of("Rapid Capture Kit bait", "0009773452", 2));
                    add(Triple.of("Rapid Capture Kit Resuspension Buffer", "0009783452", 3));
                }},
                new ArrayList<Triple<String, String, Integer>>() {{
                    add(Triple.of("Dual Index Primers Lot", "0009764452", 4));
                    add(Triple.of("Rapid Capture Enrichment Amp Lot Barcode", "0009765452", 5));
                }}
        ).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : iceJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        ArrayList<List<String>> listLcsetListNormCatchBarcodes = new ArrayList<>();
        for (int i = 0; i < lcsets.size(); i++) {
            listLcsetListNormCatchBarcodes.add(new ArrayList<String>());
        }
        for (int i = 0; i < iceJaxbBuilder.getCatchEnrichTubeBarcodes().size(); i++) {
            String tubeBarcode = iceJaxbBuilder.getCatchEnrichTubeBarcodes().get(i);
            listLcsetListNormCatchBarcodes.get(i % lcsets.size()).add(tubeBarcode);
        }

        ArrayList<String> normCatchRackBarcodes = new ArrayList<>();
        for (String testPrefix : testPrefixes) {
            normCatchRackBarcodes.add("QtpRack" + testPrefix);
        }
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefixes.get(0),
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, true, QtpJaxbBuilder.PcrType.VIIA_7);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        List<ZimsIlluminaRun> zimsIlluminaRuns = new ArrayList<>();
        for (int i = 0; i < qtpJaxbBuilder.getDenatureTubeBarcodes().size(); i++) {
            String denatureTubeBarcode = qtpJaxbBuilder.getDenatureTubeBarcodes().get(i);
            HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory,
                    testPrefixes.get(i), Collections.singletonList(denatureTubeBarcode),
                    qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1", ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                    BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
            }
            BarcodedTube poolTube = barcodedTubeDao.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcodes().get(0));
            Assert.assertEquals(poolTube.getSampleInstancesV2().size(), BaseEventTest.NUM_POSITIONS_IN_RACK,
                    "Wrong number of sample instances");

            IlluminaSequencingRun illuminaSequencingRun = registerIlluminaSequencingRun(testPrefixes.get(i),
                    hiSeq2500JaxbBuilder.getFlowcellBarcode());

            ZimsIlluminaRun zimsIlluminaRun = illuminaRunResource.getRun(illuminaSequencingRun.getRunName());
            zimsIlluminaRuns.add(zimsIlluminaRun);
        }
        return zimsIlluminaRuns;
    }

    @Nonnull
    private Pair<LibraryConstructionJaxbBuilder, Map<String, BarcodedTube>> libraryConstruction(String testPrefix,
                                                                                                BettaLimsMessageTestFactory bettaLimsMessageFactory,
                                                                                                String workflow)
            throws ValidationException {
        Map<String, BarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, workflow);
        ShearingJaxbBuilder shearingExExJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, "Shear" + testPrefix).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : shearingExExJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }

        LibraryConstructionJaxbBuilder libraryConstructionExExJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageFactory, testPrefix, shearingExExJaxbBuilder.getShearCleanPlateBarcode(),
                LibraryConstructionJaxbBuilder.P_7_INDEX_PLATE_BARCODE,
                LibraryConstructionJaxbBuilder.P_5_INDEX_PLATE_BARCODE, BaseEventTest.NUM_POSITIONS_IN_RACK,
                LibraryConstructionJaxbBuilder.TargetSystem.SQUID_VIA_MERCURY,
                Arrays.asList(Triple.of("KAPA Reagent Box", "0009753252", 1)),
                Arrays.asList(Triple.of("PEG", "0009753352", 2),
                        Triple.of("70% Ethanol", "LCEtohTest", 3),
                        Triple.of("EB", "0009753452", 4),
                        Triple.of("SPRI", "LCSpriTest", 5)),
                Arrays.asList(Triple.of("KAPA Amp Kit", "0009753250", 6)),
                LibraryConstructionJaxbBuilder.PondType.REGULAR).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionExExJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, appConfig.getUrl());
        }
        return new ImmutablePair<>(libraryConstructionExExJaxbBuilder, mapBarcodeToTube);
    }

    public static String sendMessage(BettaLIMSMessage bettaLIMSMessage,
                                     BettaLimsMessageResource bettalimsMessageResource,
                                     String testMercuryUrl) {
        String response = null;
        if (true) {
            // In JVM
            try {
                bettalimsMessageResource.storeAndProcess(BettaLimsMessageTestFactory.marshal(bettaLIMSMessage));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (false) {
            // JAX-RS
            ClientBuilder clientBuilder = ClientBuilder.newBuilder();
            JaxRsUtils.acceptAllServerCertificates(clientBuilder);

            response = clientBuilder.build().target(testMercuryUrl + "/rest/bettalimsmessage")
                    .request(MediaType.APPLICATION_XML_TYPE)
                    .post(Entity.xml(bettaLIMSMessage), String.class);
        }

        if (false) {
            // JMS
            BettaLimsMessageBeanTest.sendJmsMessage(BettaLimsMessageTestFactory.marshal(bettaLIMSMessage),
                    "broad.queue.mercury.bettalims.dev");
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return response;
    }

    /**
     * Send messages from files that were created by production BettaLIMS
     *
     * @param baseUrl URL of arquillian server
     */
    @Test(enabled = false, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testHttp(@ArquillianResource URL baseUrl) {
        File inboxDirectory = new File("C:/Temp/seq/lims/bettalims/production/inbox");
        List<String> dayDirectoryNames = Arrays.asList(inboxDirectory.list());
        Collections.sort(dayDirectoryNames);
        for (String dayDirectoryName : dayDirectoryNames) {
            if (dayDirectoryName.startsWith("2012")) {
                File dayDirectory = new File(inboxDirectory, dayDirectoryName);
                List<String> messageFileNames = Arrays.asList(dayDirectory.list());
                Collections.sort(messageFileNames);
                for (String messageFileName : messageFileNames) {
/*
                    String message = FileUtils.readFileToString(new File(dayDirectory, messageFileName));
                    if(message.contains("PreSelectionPool"))
                        BettaLimsMessageBeanTest.sendJmsMessage(message);
*/
                    sendFile(baseUrl, new File(dayDirectory, messageFileName));
                }
            }
        }
    }

    /**
     * Send a single message from a file that was created by production BettaLIMS
     *
     * @param baseUrl URL of deployed server
     */
    @Test(enabled = false, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testSingleFile(@ArquillianResource URL baseUrl) {
        File file = new File(
                "c:/Temp/seq/lims/bettalims/production/inbox/20120103/20120103_101119570_localhost_9998_ws.xml");
        sendFile(baseUrl, file);
    }

    @Test(enabled = false, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFileList(@ArquillianResource URL baseUrl) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("c:/temp/PdoLcSetMessageList.txt"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    sendFile(baseUrl, new File(line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a file from a message
     *
     * @param baseUrl URL of the server
     * @param file    message file
     */
    private void sendFile(URL baseUrl, File file) {
        try {

            ClientBuilder clientBuilder = ClientBuilder.newBuilder();
            JaxRsUtils.acceptAllServerCertificates(clientBuilder);

            String response = clientBuilder.build()
                    .target(RestServiceContainerTest.convertUrlToSecure(baseUrl) + "rest/bettalimsmessage")
                    .request(MediaType.APPLICATION_XML_TYPE)
                    .post(Entity.xml(file), String.class);
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
