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
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientEjb;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ImportFromSquidTest;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PreFlightJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ShearingJaxbBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test the web service
 */
@SuppressWarnings("OverlyCoupledClass")
public class BettalimsMessageResourceTest extends Arquillian {

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private SolexaRunResource solexaRunResource;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private AthenaClientService athenaClientService;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private IlluminaFlowcellDao flowcellDao;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private IlluminaSequencingRunFactory illuminaSequencingRunFactory;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private MercuryClientEjb mercuryClientEjb;

    @Inject
    private ReworkEjb reworkEjb;

    private final SimpleDateFormat testPrefixDateFormat = new SimpleDateFormat("MMddHHmmss");

    private static final Map<String, String> mapWorkflowToPartNum = new HashMap<String, String>();

    static {
        mapWorkflowToPartNum.put("Custom Amplicon", "P-VAL-0002");
//        ("IGN WGS", "?")
//        ("Fluidigm Multi", "?")
        mapWorkflowToPartNum.put("Whole Genome", "P-WG-0002");
        mapWorkflowToPartNum.put("Exome Express", "P-EX-0002");
        mapWorkflowToPartNum.put("Hybrid Selection", "P-EX-0001");
    }


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    /**
     * Sends messages for one PDO, then reworks two of those samples along with a second PDO.
     */
    @Test(enabled = true, groups = EXTERNAL_INTEGRATION, timeOut = 0)
    public void testRework() throws ValidationException {
        // Set up one PDO / bucket / batch
        String testPrefix = testPrefixDateFormat.format(new Date());
        ProductOrder productOrder1 = buildProductOrder(testPrefix, BaseEventTest.NUM_POSITIONS_IN_RACK,
                WorkflowName.EXOME_EXPRESS);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK);
        bucketAndBatch(testPrefix, productOrder1, mapBarcodeToTube);
        // message
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);
        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, WorkflowName.EXOME_EXPRESS);

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                WorkflowName.EXOME_EXPRESS).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), "squidTestDesignation").invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        // Create second PDO
        testPrefix = testPrefixDateFormat.format(new Date());
        ProductOrder productOrder2 = buildProductOrder(testPrefix, BaseEventTest.NUM_POSITIONS_IN_RACK - 2,
                WorkflowName.EXOME_EXPRESS);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube2 = buildSampleTubes(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK - 2);

        // Add two samples from first PDO to bucket
        Set<LabVessel> reworks = new HashSet<LabVessel>();
        Iterator<Map.Entry<String, TwoDBarcodedTube>> iterator = mapBarcodeToTube.entrySet().iterator();
        Map.Entry<String, TwoDBarcodedTube> barcodeTubeEntry = iterator.next();
        reworkEjb.addRework(barcodeTubeEntry.getValue(), ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                LabEventType.PICO_PLATING_BUCKET, "Test");
        mapBarcodeToTube2.put(barcodeTubeEntry.getKey(), barcodeTubeEntry.getValue());
        reworks.add(barcodeTubeEntry.getValue());

        barcodeTubeEntry = iterator.next();
        reworkEjb.addRework(barcodeTubeEntry.getValue(), ReworkEntry.ReworkReason.UNKNOWN_ERROR,
                LabEventType.PICO_PLATING_BUCKET, "Test");
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
                LabEvent.UI_EVENT_LOCATION);
        // message
        hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix, mapBarcodeToTube2, bettaLimsMessageFactory,
                WorkflowName.EXOME_EXPRESS);

        qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                WorkflowName.EXOME_EXPRESS).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), "squidTestDesignation").invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

    }

    /**
     * Message one LCSET, and register run.
     */
    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void testProcessMessage() {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                BaseEventTest.NUM_POSITIONS_IN_RACK, WorkflowName.EXOME_EXPRESS);

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory, WorkflowName.EXOME_EXPRESS);

        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                WorkflowName.EXOME_EXPRESS).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageFactory, testPrefix,
                qtpJaxbBuilder.getDenatureTubeBarcode(), "squidTestDesignation").invoke();
        for (BettaLIMSMessage bettaLIMSMessage : hiSeq2500JaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }
        TwoDBarcodedTube poolTube = twoDBarcodedTubeDAO.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcodes().get(0));
        Assert.assertEquals(poolTube.getSampleInstances().size(), LabEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        IlluminaSequencingRun illuminaSequencingRun = registerIlluminaSequencingRun(testPrefix,
                hiSeq2500JaxbBuilder.getFlowcellBarcode());
        ZimsIlluminaRunFactory zimsIlluminaRunFactory =
                new ZimsIlluminaRunFactory(bspSampleDataFetcher, athenaClientService);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 2, "Wrong number of lanes");

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
        String runName = "TestRun" + testPrefix + runDate.getTime();

        String runPath = "/tmp/file/run/path/" + runName + ".txt";

        IlluminaFlowcell flowcell = flowcellDao.findByBarcode(flowcellBarcode);

        SolexaRunBean solexaRunBean = new SolexaRunBean(flowcellBarcode, flowcellBarcode + format.format(runDate),
                runDate, "SL-HAL", runPath, null);
        return illuminaSequencingRunFactory.buildDbFree(solexaRunBean, flowcell);
    }

    /**
     * Send messages for Preflight, Shearing, Library Construction and Hybridization
     *
     * @param testPrefix              make barcodes unique
     * @param mapBarcodeToTube        map from tube barcode to sample tube
     * @param bettaLimsMessageFactory to build messages
     *
     * @return allows access to catch tubes
     */
    private HybridSelectionJaxbBuilder sendMessagesUptoCatch(String testPrefix,
                                                             Map<String, TwoDBarcodedTube> mapBarcodeToTube,
                                                             BettaLimsMessageTestFactory bettaLimsMessageFactory,
                                                             WorkflowName workflowName) {

        String shearingRackBarcode;
        if (workflowName == WorkflowName.EXOME_EXPRESS) {
            shearingRackBarcode = "ShearRack" + testPrefix;
        } else {
            PreFlightJaxbBuilder preFlightJaxbBuilder = new PreFlightJaxbBuilder(
                    bettaLimsMessageFactory, testPrefix, new ArrayList<String>(mapBarcodeToTube.keySet())).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : preFlightJaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage);
            }
            shearingRackBarcode = preFlightJaxbBuilder.getRackBarcode();
        }
        ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageFactory,
                new ArrayList<String>(mapBarcodeToTube.keySet()), testPrefix, shearingRackBarcode).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        Map<String, StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseStream(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("DuplexCOAforBroad.xlsx"),
                IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        StaticPlate indexPlate = mapBarcodeToPlate.values().iterator().next();
        if (staticPlateDAO.findByBarcode(indexPlate.getLabel()) == null) {
            staticPlateDAO.persist(indexPlate);
        }

        LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                bettaLimsMessageFactory, testPrefix, shearingJaxbBuilder.getShearCleanPlateBarcode(),
                indexPlate.getLabel(),
                LabEventTest.NUM_POSITIONS_IN_RACK).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(bettaLimsMessageFactory,
                testPrefix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                libraryConstructionJaxbBuilder.getPondRegTubeBarcodes(), "Bait" + testPrefix).invoke();
        List<ReagentDesign> reagentDesigns = reagentDesignDao.findAll(ReagentDesign.class, 0, 1);
        ReagentDesign baitDesign = null;
        if (reagentDesigns != null && !reagentDesigns.isEmpty()) {
            baitDesign = reagentDesigns.get(0);
        }

        twoDBarcodedTubeDAO.persist(LabEventTest.buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(),
                baitDesign));

        for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
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
                                                            WorkflowName workflowName) {
        ProductOrder productOrder = buildProductOrder(testPrefix, numberOfSamples, workflowName);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSampleTubes(testPrefix, numberOfSamples);
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
    private ProductOrder buildProductOrder(String testPrefix, int numberOfSamples, WorkflowName workflowName) {
        String partNumber = mapWorkflowToPartNum.get(workflowName.getWorkflowName());
        Product product = productDao.findByPartNumber(partNumber);
        if (product == null) {
            // todo jmt change to exome express
            product = new Product("Standard Exome Sequencing", productFamilyDao.find("Exome"),
                    "Standard Exome Sequencing", "P-EX-0001", new Date(), null, 1814400, 1814400, 184, null, null,
                    null, true, WorkflowName.HYBRID_SELECTION.getWorkflowName(), false, "agg type");
            product.setPrimaryPriceItem(new PriceItem("1234", PriceItem.PLATFORM_GENOMICS, "Pony Genomics",
                    "Standard Pony"));
            productDao.persist(product);
        }

        ResearchProject researchProject = researchProjectDao.findByBusinessKey("RP-19");
        if (researchProject == null) {
            researchProject = new ResearchProject(10950L, "SIGMA Sarcoma", "SIGMA Sarcoma", false);
            researchProjectDao.persist(researchProject);
        }

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();
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
     * @param testPrefix      make unique
     * @param numberOfSamples how many samples
     *
     * @return map from tube barcode to tube
     */
    private Map<String, TwoDBarcodedTube> buildSampleTubes(String testPrefix, int numberOfSamples) {
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for (int rackPosition = 1; rackPosition <= numberOfSamples; rackPosition++) {
            String barcode = "R" + testPrefix + rackPosition;
            String bspStock = "SM-" + testPrefix + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);

            twoDBarcodedTubeDAO.persist(bspAliquot);
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
        labBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), batchName));
        labBatchEjb.createLabBatchAndRemoveFromBucket(labBatch, "jowalsh", "Pico/Plating Bucket",
                LabEvent.UI_EVENT_LOCATION);
    }

    /**
     * Test performance by creating a flowcell with a different 96 sample LCSET on each of 8 lanes.
     * Needs -Dorg.jboss.remoting-jmx.timeout=3000
     */
    @Test(enabled = false)
    public void test8Lcsets() {
        String testPrefix;
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory(true);
        List<List<String>> listLcsetListNormCatchBarcodes = new ArrayList<List<String>>();
        List<String> normCatchRackBarcodes = new ArrayList<String>();

        // Get to catch, for 8 LCSETs
        for (int i = 0; i < 8; i++) {
            testPrefix = testPrefixDateFormat.format(new Date());
            Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix,
                    BaseEventTest.NUM_POSITIONS_IN_RACK, WorkflowName.HYBRID_SELECTION);
            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder =
                    sendMessagesUptoCatch(testPrefix, mapBarcodeToTube, bettaLimsMessageFactory,
                            WorkflowName.HYBRID_SELECTION);
            listLcsetListNormCatchBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchBarcodes());
            normCatchRackBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchRackBarcode());
        }

        // Combine 8 LCSETs on one flowcell
        testPrefix = testPrefixDateFormat.format(new Date());
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                listLcsetListNormCatchBarcodes,
                normCatchRackBarcodes,
                WorkflowName.HYBRID_SELECTION).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        IlluminaSequencingRun illuminaSequencingRun =
                registerIlluminaSequencingRun(testPrefix, qtpJaxbBuilder.getFlowcellBarcode());
        ZimsIlluminaRunFactory zimsIlluminaRunFactory =
                new ZimsIlluminaRunFactory(bspSampleDataFetcher, athenaClientService);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
    }

    private void sendMessage(BettaLIMSMessage bettaLIMSMessage) {
        if (true) {
            // In JVM
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
        }

        if (false) {
            // JAX-RS
            String response = Client.create().resource(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(bettaLIMSMessage)
                    .post(String.class);
        }

        if (false) {
            // JMS
            BettalimsMessageBeanTest.sendJmsMessage(BettalimsMessageBeanTest.marshalMessage(bettaLIMSMessage));
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
                        BettalimsMessageBeanTest.sendJmsMessage(message);
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
