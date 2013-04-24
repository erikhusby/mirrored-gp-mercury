package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

//import com.jprofiler.api.agent.Controller;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
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
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionJaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    private final SimpleDateFormat testPrefixDateFormat=new SimpleDateFormat("MMddHHmmss");

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod(groups = EXTERNAL_INTEGRATION)
    public void setUp() throws SystemException, NotSupportedException {
        // Skip if no injections, meaning we're not running in container
        if(utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = EXTERNAL_INTEGRATION)
    public void tearDown() {
        // Skip if no injections, meaning we're not running in container
        if(utx == null) {
            return;
        }

        // Ignore all exceptions, to preserve exception that was generated by the test
        //noinspection OverlyBroadCatchBlock
        try {
            utx.commit();
        } catch (Exception ignored) {
        }
    }

    /**
     * Message one LCSET, and register run.
     */
    @Test(enabled = false, groups = EXTERNAL_INTEGRATION)
    public void testProcessMessage() {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix);

        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory();

        HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = sendMessagesUptoCatch(testPrefix,
                mapBarcodeToTube, bettaLimsMessageFactory);

        QtpJaxbBuilder qtpJaxbBuilder=new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchBarcodes()),
                Collections.singletonList(hybridSelectionJaxbBuilder.getNormCatchRackBarcode()),
                WorkflowName.HYBRID_SELECTION).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

//        Controller.stopCPURecording();
        TwoDBarcodedTube poolTube = twoDBarcodedTubeDAO.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcodes().get(0));
        Assert.assertEquals(poolTube.getSampleInstances().size(), LabEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        IlluminaSequencingRun illuminaSequencingRun = registerIlluminaSequencingRun(testPrefix, qtpJaxbBuilder.getFlowcellBarcode());
        ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(bspSampleDataFetcher, athenaClientService);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
    }

    /**
     * Register a flowcell run
     * @param testPrefix make barcodes unique
     * @param flowcellBarcode flowcell to register
     * @return registered run
     */
    private IlluminaSequencingRun registerIlluminaSequencingRun(String testPrefix, String flowcellBarcode) {
        Date runDate = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd");
        String runName="TestRun" + testPrefix + runDate.getTime();

        String runPath = "/tmp/file/run/path/"+ runName + ".txt";

        IlluminaFlowcell flowcell = flowcellDao.findByBarcode(flowcellBarcode);

        return illuminaSequencingRunFactory.buildDbFree(
                new SolexaRunBean(flowcellBarcode,
                        flowcellBarcode + format.format(runDate),
                        runDate, "SL-HAL", runPath, null),
                flowcell);
    }

    /**
     * Send messages for Preflight, Shearing, Library Construction and Hybridization
     * @param testPrefix make barcodes unique
     * @param mapBarcodeToTube map from tube barcode to sample tube
     * @param bettaLimsMessageFactory to build messages
     * @return allows access to catch tubes
     */
    private HybridSelectionJaxbBuilder sendMessagesUptoCatch(String testPrefix,
            Map<String, TwoDBarcodedTube> mapBarcodeToTube,
            BettaLimsMessageTestFactory bettaLimsMessageFactory) {
        try {
            PreFlightJaxbBuilder preFlightJaxbBuilder=new PreFlightJaxbBuilder(
                    bettaLimsMessageFactory, testPrefix, new ArrayList<String>(mapBarcodeToTube.keySet())).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : preFlightJaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage);
            }

            ShearingJaxbBuilder shearingJaxbBuilder=new ShearingJaxbBuilder(bettaLimsMessageFactory,
                    new ArrayList<String>(mapBarcodeToTube.keySet()), testPrefix, preFlightJaxbBuilder.getRackBarcode()).invoke();
            for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage);
            }

            Map<String,StaticPlate> mapBarcodeToPlate=indexedPlateFactory.parseStream(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("DuplexCOAforBroad.xlsx"),
                    IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
            StaticPlate indexPlate=mapBarcodeToPlate.values().iterator().next();
            if(staticPlateDAO.findByBarcode(indexPlate.getLabel()) == null) {
                staticPlateDAO.persist(indexPlate);
                staticPlateDAO.flush();
                staticPlateDAO.clear();
            }
            utx.commit();
            utx.begin();
            LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LibraryConstructionJaxbBuilder(
                    bettaLimsMessageFactory, testPrefix, shearingJaxbBuilder.getShearCleanPlateBarcode(), indexPlate.getLabel(),
                    LabEventTest.NUM_POSITIONS_IN_RACK).invoke();

            for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage);
            }

            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new HybridSelectionJaxbBuilder(bettaLimsMessageFactory,
                    testPrefix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                    libraryConstructionJaxbBuilder.getPondRegTubeBarcodes(), "Bait" + testPrefix).invoke();
            List<ReagentDesign> reagentDesigns=reagentDesignDao.findAll(ReagentDesign.class, 0, 1);
            ReagentDesign baitDesign=null;
            if(reagentDesigns != null && !reagentDesigns.isEmpty()) {
                baitDesign=reagentDesigns.get(0);
            }

            twoDBarcodedTubeDAO.persist(LabEventTest.buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode(), baitDesign));
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
            utx.commit();
            utx.begin();
            for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
                sendMessage(bettaLIMSMessage);
            }
            return hybridSelectionJaxbBuilder;
        } catch (RollbackException e) {
            throw new RuntimeException(e);
        } catch (HeuristicMixedException e) {
            throw new RuntimeException(e);
        } catch (HeuristicRollbackException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build samples in plastic, and associate them with a PDO
     * @param testPrefix make barcodes unique
     * @return map from tube barcode to sample tube
     */
    private Map<String, TwoDBarcodedTube> buildSamplesInPdo(String testPrefix) {
        List<ProductOrderSample> productOrderSamples=new ArrayList<ProductOrderSample>();
        for (int rackPosition=1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String bspStock="SM-" + testPrefix + rackPosition;
            productOrderSamples.add(new ProductOrderSample(bspStock));
        }

        Product exomeExpressProduct=productDao.findByPartNumber("P-EX-0001");
        if(exomeExpressProduct == null) {
            exomeExpressProduct=new Product("Standard Exome Sequencing", productFamilyDao.find("Exome"),
                    "Standard Exome Sequencing", "P-EX-0001", new Date(), null, 1814400, 1814400, 184, null, null,
                    null, true, WorkflowName.HYBRID_SELECTION.getWorkflowName(), false, "agg type");
            exomeExpressProduct.setPrimaryPriceItem(new PriceItem("1234", PriceItem.PLATFORM_GENOMICS, "Pony Genomics",
                    "Standard Pony"));
            productDao.persist(exomeExpressProduct);
            productDao.flush();
        }
        ResearchProject researchProject=researchProjectDao.findByBusinessKey("RP-19");
        if(researchProject == null) {
            researchProject=new ResearchProject(10950L, "SIGMA Sarcoma", "SIGMA Sarcoma", false);
            researchProjectDao.persist(researchProject);
        }
        ProductOrder productOrder=new ProductOrder(10950L, "Messaging Test " + testPrefix, productOrderSamples, "GSP-123",
                exomeExpressProduct, researchProject);
        productOrder.prepareToSave(bspUserList.getByUsername("jowalsh"));
        productOrderDao.persist(productOrder);
        try {
            productOrder.placeOrder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String,TwoDBarcodedTube> mapBarcodeToTube=new LinkedHashMap<String,TwoDBarcodedTube>();
        HashSet<LabVessel> starters = new HashSet<LabVessel>();
        for (int rackPosition=1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode="R" + testPrefix + rackPosition;
            String bspStock="SM-" + testPrefix + rackPosition;
            TwoDBarcodedTube bspAliquot=new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);

            twoDBarcodedTubeDAO.persist(bspAliquot);
            starters.add(bspAliquot);
        }

        String batchName = "LCSET-MsgTest-" + testPrefix;
        LabBatch labBatch = new LabBatch(batchName, starters, LabBatch.LabBatchType.WORKFLOW);
        labBatch.setJiraTicket(new JiraTicket(JiraServiceProducer.stubInstance(), batchName));

        labBatchEjb.createLabBatch(labBatch, "jowalsh");
        productOrderDao.flush();
        productOrderDao.clear();
        try {
            utx.commit();
            utx.begin();
        } catch (RollbackException e) {
            throw new RuntimeException(e);
        } catch (HeuristicMixedException e) {
            throw new RuntimeException(e);
        } catch (HeuristicRollbackException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        }
        return mapBarcodeToTube;
    }

    /**
     * Test performance by creating a flowcell with a different 96 sample LCSET on each of 8 lanes.
     * Needs -Dorg.jboss.remoting-jmx.timeout=3000
     */
    @Test(enabled = false)
    public void test8Lcsets() {
        String testPrefix;
        BettaLimsMessageTestFactory bettaLimsMessageFactory = new BettaLimsMessageTestFactory();
        List<List<String>> listLcsetListNormCatchBarcodes = new ArrayList<List<String>>();
        List<String> normCatchRackBarcodes = new ArrayList<String>();

        // Get to catch, for 8 LCSETs
        for (int i = 0; i < 8; i++) {
            testPrefix = testPrefixDateFormat.format(new Date());
            Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildSamplesInPdo(testPrefix);
            HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder =
                    sendMessagesUptoCatch(testPrefix, mapBarcodeToTube, bettaLimsMessageFactory);
            listLcsetListNormCatchBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchBarcodes());
            normCatchRackBarcodes.add(hybridSelectionJaxbBuilder.getNormCatchRackBarcode());
        }

        // Combine 8 LCSETs on one flowcell
        testPrefix = testPrefixDateFormat.format(new Date());
        QtpJaxbBuilder qtpJaxbBuilder=new QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                listLcsetListNormCatchBarcodes,
                normCatchRackBarcodes,
                WorkflowName.HYBRID_SELECTION).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        IlluminaSequencingRun illuminaSequencingRun = registerIlluminaSequencingRun(testPrefix, qtpJaxbBuilder.getFlowcellBarcode());
        ZimsIlluminaRunFactory zimsIlluminaRunFactory = new ZimsIlluminaRunFactory(bspSampleDataFetcher, athenaClientService);
        ZimsIlluminaRun zimsIlluminaRun = zimsIlluminaRunFactory.makeZimsIlluminaRun(illuminaSequencingRun);
        Assert.assertEquals(zimsIlluminaRun.getLanes().size(), 8, "Wrong number of lanes");
    }

    private void sendMessage(BettaLIMSMessage bettaLIMSMessage) {
        if (true) {
            // In JVM
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
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
     * @param baseUrl URL of arquillian server
     */
    @Test(enabled=false, groups=EXTERNAL_INTEGRATION, dataProvider=Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testHttp(@ArquillianResource URL baseUrl) {
        File inboxDirectory=new File("C:/Temp/seq/lims/bettalims/production/inbox");
        List<String> dayDirectoryNames=Arrays.asList(inboxDirectory.list());
        Collections.sort(dayDirectoryNames);
        for (String dayDirectoryName : dayDirectoryNames) {
            if(dayDirectoryName.startsWith("2012")) {
                File dayDirectory=new File(inboxDirectory, dayDirectoryName);
                List<String> messageFileNames=Arrays.asList(dayDirectory.list());
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
     * @param baseUrl URL of deployed server
     */
    @Test(enabled=false, groups=EXTERNAL_INTEGRATION, dataProvider=Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testSingleFile(@ArquillianResource URL baseUrl) {
        File file = new File("c:/Temp/seq/lims/bettalims/production/inbox/20120103/20120103_101119570_localhost_9998_ws.xml");
        sendFile(baseUrl, file);
    }

    @Test(enabled=false, groups=EXTERNAL_INTEGRATION, dataProvider=Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFileList(@ArquillianResource URL baseUrl) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("c:/temp/PdoLcSetMessageList.txt"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if(!line.startsWith("#")) {
                    sendFile(baseUrl, new File(line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a file from a message
     * @param baseUrl URL of the server
     * @param file message file
     */
    private void sendFile(URL baseUrl, File file) {
        try {
            String response= Client.create().resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
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
