package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

//import com.jprofiler.api.agent.Controller;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientEjb;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.IlluminaRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ImportFromSquidTest;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingJaxbBuilder;
import org.broadinstitute.gpinformatics.mocks.EverythingYouAskForYouGetAndItsHuman;
import org.easymock.EasyMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test the web service
 */
@SuppressWarnings("OverlyCoupledClass")
public class BettaLimsMessageResourceTest extends Arquillian {

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private SolexaRunResource solexaRunResource;

    @Inject
    private TwoDBarcodedTubeDao twoDBarcodedTubeDao;

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
    private MercuryClientEjb mercuryClientEjb;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private IlluminaRunResource illuminaRunResource;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    private final SimpleDateFormat testPrefixDateFormat = new SimpleDateFormat("MMddHHmmss");

    public static final Map<Workflow, String> mapWorkflowToPartNum = new EnumMap<Workflow, String>(Workflow.class) {{
        put(Workflow.WHOLE_GENOME, "P-WG-0002");
        put(Workflow.AGILENT_EXOME_EXPRESS, "P-EX-0002");
        put(Workflow.HYBRID_SELECTION, "P-EX-0001");
    }};


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, EverythingYouAskForYouGetAndItsHuman.class);
    }

    /**
     * Sends messages for one PDO, then reworks two of those samples along with a second PDO.
     */
    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void testRework() throws ValidationException {
        // Set up one PDO / bucket / batch
        String testPrefix = testPrefixDateFormat.format(new Date());
        ProductOrder productOrder1 = buildProductOrder(testPrefix, BaseEventTest.NUM_POSITIONS_IN_RACK,
                Workflow.AGILENT_EXOME_EXPRESS);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, twoDBarcodedTubeDao);
        bucketAndBatch(testPrefix, productOrder1, mapBarcodeToTube);
        // message
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);
        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, twoDBarcodedTubeDao,
                ImportFromSquidTest.TEST_MERCURY_URL, BaseEventTest.NUM_POSITIONS_IN_RACK);

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                false).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1",
                ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }

        // Create second PDO
        testPrefix = testPrefixDateFormat.format(new Date());
        ProductOrder productOrder2 = buildProductOrder(testPrefix, BaseEventTest.NUM_POSITIONS_IN_RACK - 2,
                Workflow.AGILENT_EXOME_EXPRESS);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube2 = buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK - 2, twoDBarcodedTubeDao);

        // Add two samples from first PDO to bucket
        Set<LabVessel> reworks = new HashSet<>();
        Iterator<Map.Entry<String, TwoDBarcodedTube>> iterator = mapBarcodeToTube.entrySet().iterator();
        Map.Entry<String, TwoDBarcodedTube> barcodeTubeEntry = iterator.next();
        reworkEjb.addAndValidateRework(new ReworkEjb.BucketCandidate(barcodeTubeEntry.getValue().getLabel(), true),
                ReworkEntry.ReworkReason.UNKNOWN_ERROR, "Pico/Plating Bucket", "Test",
                Workflow.AGILENT_EXOME_EXPRESS, "jowalsh");
        mapBarcodeToTube2.put(barcodeTubeEntry.getKey(), barcodeTubeEntry.getValue());
        reworks.add(barcodeTubeEntry.getValue());

        barcodeTubeEntry = iterator.next();
        reworkEjb.addAndValidateRework(new ReworkEjb.BucketCandidate(barcodeTubeEntry.getValue().getLabel(), true),
                ReworkEntry.ReworkReason.UNKNOWN_ERROR, "Pico/Plating Bucket", "Test",
                Workflow.AGILENT_EXOME_EXPRESS, "jowalsh");
        mapBarcodeToTube2.put(barcodeTubeEntry.getKey(), barcodeTubeEntry.getValue());
        reworks.add(barcodeTubeEntry.getValue());

        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube2.values());
        mercuryClientEjb.addFromProductOrder(productOrder2);

        // Create batch
        String batchName = "LCSET-MsgTest-" + testPrefix;
        LabBatch labBatch = new LabBatch(batchName, starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.addReworks(reworks);
        labBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), batchName));
        labBatchEjb.createLabBatchAndRemoveFromBucket(labBatch, "jowalsh", "Pico/Plating Bucket",
                LabEvent.UI_EVENT_LOCATION, CreateFields.IssueType.EXOME_EXPRESS);
        // message
        hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix, mapBarcodeToTube2, bettaLimsMessageFactory,
                Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, twoDBarcodedTubeDao, ImportFromSquidTest.TEST_MERCURY_URL,
                BaseEventTest.NUM_POSITIONS_IN_RACK);

        qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                false).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }

        hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1",
                ProductionFlowcellPath.DENATURE_TO_FLOWCELL,
                BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }

    }

    /**
     * Message one LCSET, and register run.
     */
    @Test(enabled = true, groups = EXTERNAL_INTEGRATION)
    public void testProcessMessage() {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, Workflow.AGILENT_EXOME_EXPRESS);

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, Workflow.AGILENT_EXOME_EXPRESS, bettaLimsMessageResource,
                reagentDesignDao, twoDBarcodedTubeDao,
                ImportFromSquidTest.TEST_MERCURY_URL, BaseEventTest.NUM_POSITIONS_IN_RACK);

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                false).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), qtpJaxbBuilder.getDenatureRackBarcode(), "FCT-1",
                ProductionFlowcellPath.DENATURE_TO_FLOWCELL, BaseEventTest.NUM_POSITIONS_IN_RACK, null, 2).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }
        TwoDBarcodedTube poolTube = twoDBarcodedTubeDao.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcodes().get(0));
        Assert.assertEquals(poolTube.getSampleInstances().size(), BaseEventTest.NUM_POSITIONS_IN_RACK,
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

        for (TwoDBarcodedTube tube:mapBarcodeToTube.values()) {
            for (BucketEntry entry:tube.getBucketEntries()) {
                entry.setStatus(BucketEntry.Status.Archived);
            }
        }

//        Controller.stopCPURecording();
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
     * @param twoDBarcodedTubeDao
     * @param testMercuryUrl
     * @param numPositionsInRack
     * @param testPrefix               make barcodes unique
     * @param mapBarcodeToTube         map from tube barcode to sample tube
     * @param bettaLimsMessageFactory  to build messages
     *
     * @return allows access to catch tubes
     */
    public static HybridSelectionJaxbBuilder sendMessagesUptoCatch(String testPrefix,
                                                                   Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                                                                   BettaLimsMessageTestFactory bettaLimsMessageFactory,
                                                                   Workflow workflow,
                                                                   BettaLimsMessageResource bettalimsMessageResource,
                                                                   ReagentDesignDao reagentDesignDao,
                                                                   TwoDBarcodedTubeDao twoDBarcodedTubeDao,
                                                                   String testMercuryUrl, int numPositionsInRack) {

        String shearingRackBarcode;
        if (workflow == Workflow.AGILENT_EXOME_EXPRESS) {
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
                LibraryConstructionJaxbBuilder.P_5_INDEX_PLATE_BARCODE, numPositionsInRack).invoke();

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

        twoDBarcodedTubeDao.persist(LabEventTest.buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(),
                baitDesign));

        for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
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
    private Map<String, TwoDBarcodedTube> buildSamplesInPdo(String testPrefix, int numberOfSamples,
                                                            Workflow workflow) {
        ProductOrder productOrder = buildProductOrder(testPrefix, numberOfSamples, workflow);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSampleTubes(testPrefix, numberOfSamples,
                twoDBarcodedTubeDao);
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
    private ProductOrder buildProductOrder(String testPrefix, int numberOfSamples, Workflow workflow) {
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

        ResearchProject researchProject = researchProjectDao.findByBusinessKey("RP-19");
        if (researchProject == null) {
            researchProject = new ResearchProject(10950L, "SIGMA Sarcoma", "SIGMA Sarcoma", false);
            researchProjectDao.persist(researchProject);
        }

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= numberOfSamples; rackPosition++) {
            String bspStock = "SM-" + testPrefix + rackPosition;
            productOrderSamples.add(new ProductOrderSample(bspStock));
        }

        ProductOrder productOrder =
                new ProductOrder(10950L, "Messaging Test " + testPrefix, productOrderSamples, "GSP-123",
                        product, researchProject);
        productOrder.prepareToSave(bspUserList.getByUsername("jowalsh"));
        productOrderDao.persist(productOrder);
        try {
            productOrder.placeOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return productOrder;
    }

    /**
     * Build samples and tubes
     *
     *
     * @param testPrefix      make unique
     * @param numberOfSamples how many samples
     *
     * @param twoDBarcodedTubeDao
     * @return map from tube barcode to tube
     */
    public static Map<String, TwoDBarcodedTube> buildSampleTubes(String testPrefix, int numberOfSamples,
                                                                 TwoDBarcodedTubeDao twoDBarcodedTubeDao) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int rackPosition = 1; rackPosition <= numberOfSamples; rackPosition++) {
            String barcode = "R" + testPrefix + rackPosition;
            String bspStock = "SM-" + testPrefix + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);

            twoDBarcodedTubeDao.persist(bspAliquot);
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
                                Map<String, TwoDBarcodedTube> mapBarcodeToTube) {
        HashSet<LabVessel> starters = new HashSet<LabVessel>(mapBarcodeToTube.values());
        mercuryClientEjb.addFromProductOrder(productOrder);

        String batchName = "LCSET-MsgTest-" + testPrefix;
        LabBatch labBatch = new LabBatch(batchName, starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.setValidationBatch(true);
        labBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        labBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), batchName));
        labBatchEjb.createLabBatchAndRemoveFromBucket(labBatch, "jowalsh", "Pico/Plating Bucket",
                LabEvent.UI_EVENT_LOCATION, CreateFields.IssueType.EXOME_EXPRESS);
    }

    /**
     * Test performance by creating a flowcell with a different 96 sample LCSET on each of 8 lanes.
     * Needs -Dorg.jboss.remoting-jmx.timeout=3000
     */
    @Test(enabled = false)
    public void test8Lcsets() {
        String testPrefix;
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);
        List<List<String>> listLcsetListNormCatchBarcodes = new ArrayList<>();
        List<String> normCatchRackBarcodes = new ArrayList<>();

        // Get to catch, for 8 LCSETs
        for (int i = 0; i < 8; i++) {
            testPrefix = testPrefixDateFormat.format(new Date());
            Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                    BaseEventTest.NUM_POSITIONS_IN_RACK, Workflow.HYBRID_SELECTION);
            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder =
                    sendMessagesUptoCatch(testPrefix, mapBarcodeToTube, bettaLimsMessageFactory,
                            Workflow.HYBRID_SELECTION, bettaLimsMessageResource,
                            reagentDesignDao, twoDBarcodedTubeDao, ImportFromSquidTest.TEST_MERCURY_URL,
                            BaseEventTest.NUM_POSITIONS_IN_RACK);
            listLcsetListNormCatchBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchBarcodes());
            normCatchRackBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchRackBarcode());
        }

        // Combine 8 LCSETs on one flowcell
        testPrefix = testPrefixDateFormat.format(new Date());
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                listLcsetListNormCatchBarcodes,
                normCatchRackBarcodes,
                false).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage, bettaLimsMessageResource, ImportFromSquidTest.TEST_MERCURY_URL);
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder =
                new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix, qtpJaxbBuilder.getDenatureTubeBarcode(),
                        qtpJaxbBuilder.getDenatureRackBarcode(), null, ProductionFlowcellPath.STRIPTUBE_TO_FLOWCELL,
                        BaseEventTest.NUM_POSITIONS_IN_RACK, null, 8);

        IlluminaSequencingRun illuminaSequencingRun =
                registerIlluminaSequencingRun(testPrefix, hiSeq2500JaxbBuilder.getFlowcellBarcode());
        Assert.assertEquals(illuminaSequencingRun.getSampleCartridge().getSampleInstances().size(),
                BaseEventTest.NUM_POSITIONS_IN_RACK * 8, "Wrong number of sample instances");
        ZimsIlluminaRun zimsIlluminaRun = illuminaRunResource.getRun(illuminaSequencingRun.getRunName());
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
    }

    public static void sendMessage(BettaLIMSMessage bettaLIMSMessage, BettaLimsMessageResource bettalimsMessageResource,
                                   String testMercuryUrl) {
        if (true) {
            // In JVM
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
        }

        if (false) {
            // JAX-RS
            String response = Client.create().resource(testMercuryUrl + "/rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(bettaLIMSMessage)
                    .post(String.class);
        }

        if (false) {
            // JMS
            BettaLimsMessageBeanTest.sendJmsMessage(BettaLimsMessageBeanTest.marshalMessage(bettaLIMSMessage));
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Send messages from files that were created by production BettaLIMS
     *
     * @param baseUrl URL of arquillian server
     */
    @Test(enabled = false, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
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
    @Test(enabled = false, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testSingleFile(@ArquillianResource URL baseUrl) {
        File file = new File(
                "c:/Temp/seq/lims/bettalims/production/inbox/20120103/20120103_101119570_localhost_9998_ws.xml");
        sendFile(baseUrl, file);
    }

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
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
            String response = Client.create().resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(file)
                    .post(String.class);
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
