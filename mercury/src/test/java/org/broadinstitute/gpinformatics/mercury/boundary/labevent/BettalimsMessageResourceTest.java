package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

//import com.jprofiler.api.agent.Controller;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.vessel.IndexedPlateFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
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
import java.io.File;
//import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

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
        try {
            utx.commit();
        } catch (RollbackException e) {
//            throw new RuntimeException(e);
        } catch (HeuristicMixedException e) {
//            throw new RuntimeException(e);
        } catch (HeuristicRollbackException e) {
//            throw new RuntimeException(e);
        } catch (SecurityException e) {
//            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
//            throw new RuntimeException(e);
        } catch (SystemException e) {
//            throw new RuntimeException(e);
        }
    }

    @Test(enabled = true, groups = EXTERNAL_INTEGRATION)
    public void testProcessMessage() throws SystemException, RollbackException, HeuristicRollbackException, HeuristicMixedException, NotSupportedException {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        String jiraTicketKey="PD0-MsgTest" + testPrefix;
        List<ProductOrderSample> productOrderSamples=new ArrayList<ProductOrderSample>();
        Map<String,TwoDBarcodedTube> mapBarcodeToTube=new LinkedHashMap<String,TwoDBarcodedTube>();
        for (int rackPosition=1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode="R" + testPrefix + rackPosition;

            String bspStock="SM-" + testPrefix + rackPosition;
            TwoDBarcodedTube bspAliquot=new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
            productOrderSamples.add(new ProductOrderSample(bspStock));

            twoDBarcodedTubeDAO.persist(bspAliquot);
        }

        Product exomeExpressProduct=productDao.findByPartNumber("P-EX-0002");
        if(exomeExpressProduct == null) {
            exomeExpressProduct=new Product("Exome Express", productFamilyDao.find("Exome"), "Exome Express",
                    "P-EX-0002", new Date(), null, 1814400, 1814400, 184, null, null, null, true, "Exome Express", false);
            exomeExpressProduct.setPrimaryPriceItem(new PriceItem("1234", PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Standard Pony"));
            productDao.persist(exomeExpressProduct);
            productDao.flush();
        }
        ResearchProject researchProject=researchProjectDao.findByBusinessKey("RP-19");
        if(researchProject == null) {
            researchProject=new ResearchProject(101L, "SIGMA Sarcoma", "SIGMA Sarcoma", false);
            researchProjectDao.persist(researchProject);
        }
        ProductOrder productOrder=new ProductOrder(101L, "Messaging Test " + testPrefix, productOrderSamples, "GSP-123",
                exomeExpressProduct, researchProject);
        productOrder.setJiraTicketKey(jiraTicketKey);
        productOrderDao.persist(productOrder);
        productOrderDao.flush();
        productOrderDao.clear();
        utx.commit();
        utx.begin();

        BettaLimsMessageFactory bettaLimsMessageFactory=new BettaLimsMessageFactory();

        LabEventTest.PreFlightJaxbBuilder preFlightJaxbBuilder=new LabEventTest.PreFlightJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                new ArrayList<String>(mapBarcodeToTube.keySet())).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : preFlightJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        LabEventTest.ShearingJaxbBuilder shearingJaxbBuilder=new LabEventTest.ShearingJaxbBuilder(bettaLimsMessageFactory,
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
        LabEventTest.LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LabEventTest.LibraryConstructionJaxbBuilder(
                bettaLimsMessageFactory, testPrefix, shearingJaxbBuilder.getShearCleanPlateBarcode(), indexPlate.getLabel(),
                LabEventTest.NUM_POSITIONS_IN_RACK).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }

        LabEventTest.HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder=new LabEventTest.HybridSelectionJaxbBuilder(bettaLimsMessageFactory,
                testPrefix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                libraryConstructionJaxbBuilder.getPondRegTubeBarcodes()).invoke();
        final List<ReagentDesign> reagentDesigns=reagentDesignDao.findAll(ReagentDesign.class, 0, 1);
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

        LabEventTest.QtpJaxbBuilder qtpJaxbBuilder=new LabEventTest.QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                hybridSelectionJaxbBuilder.getNormCatchBarcodes(), hybridSelectionJaxbBuilder.getNormCatchRackBarcode()).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            sendMessage(bettaLIMSMessage);
        }
//        Controller.stopCPURecording();
        TwoDBarcodedTube poolTube = twoDBarcodedTubeDAO.findByBarcode(qtpJaxbBuilder.getPoolTubeBarcode());
        Assert.assertEquals(poolTube.getSampleInstances().size(), LabEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        String runName="TestRun" + testPrefix;
        try {
            solexaRunResource.registerRun(new SolexaRunBean(qtpJaxbBuilder.getFlowcellBarcode(), runName, new Date(), "SL-HAL",
                    File.createTempFile("RunDir", ".txt").getAbsolutePath(), null));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Test run name " + runName);
    }

    private void sendMessage(BettaLIMSMessage bettaLIMSMessage) {
        bettalimsMessageResource.processMessage(bettaLIMSMessage);
        twoDBarcodedTubeDAO.flush();
        twoDBarcodedTubeDAO.clear();
//        String response = Client.create().resource(ImportFromSquidTest.TEST_MERCURY_URL + "/rest/bettalimsmessage")
//                .type(MediaType.APPLICATION_XML_TYPE)
//                .accept(MediaType.APPLICATION_XML)
//                .entity(bettaLIMSMessage)
//                .post(String.class);
//        BettalimsMessageBeanTest.sendJmsMessage(BettalimsMessageBeanTest.marshalMessage(bettaLIMSMessage));
    }

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
                    String response=null;
                    try {
                        //                    String message = FileUtils.readFileToString(new File(dayDirectory, messageFileName));
                        //                    if(message.contains("PreSelectionPool")) {
/*
                        String message;
                        FileInputStream stream = new FileInputStream(new File(dayDirectory, messageFileName));
                        try {
                            FileChannel fc = stream.getChannel();
                            MappedByteBuffer mappedByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                            message = Charset.defaultCharset().decode(mappedByteBuffer).toString();
                        }
                        finally {
                            stream.close();
                        }
                        BettalimsMessageBeanTest.sendJmsMessage(message);
*/
                        response=Client.create().resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                                .type(MediaType.APPLICATION_XML_TYPE)
                                .accept(MediaType.APPLICATION_XML)
                                .entity(new File(dayDirectory, messageFileName))
                                .post(String.class);
                        //                    }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println(response);
                }
            }
        }
    }

    @Test(enabled=false, groups=EXTERNAL_INTEGRATION, dataProvider=Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testSingleFile(@ArquillianResource URL baseUrl) {
        String response=null;
        try {
            response=Client.create().resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(new File("c:/Temp/seq/lims/bettalims/production/inbox/20120103/20120103_101119570_localhost_9998_ws.xml"))
                    .post(String.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println(response);
    }

}
