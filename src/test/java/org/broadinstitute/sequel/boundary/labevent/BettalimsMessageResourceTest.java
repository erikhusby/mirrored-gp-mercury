package org.broadinstitute.sequel.boundary.labevent;

//import com.jprofiler.api.agent.Controller;
import com.sun.jersey.api.client.Client;
import org.broadinstitute.sequel.boundary.Sample;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.test.BettaLimsMessageFactory;
import org.broadinstitute.sequel.test.LabEventTest;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.sequel.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.control.vessel.IndexedPlateFactory;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.run.IlluminaSequencingRun;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Test the web service
 */
@SuppressWarnings("OverlyCoupledClass")
public class BettalimsMessageResourceTest extends ContainerTest {

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private IndexedPlateFactory indexedPlateFactory;

    private final SimpleDateFormat testPrefixDateFormat = new SimpleDateFormat("MMddHHmmss");

    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testProcessMessage() {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project project = new BasicProject(testPrefix + "LabEventTesting", new JiraTicket(new DummyJiraService(),
                "TP-" + testPrefix, testPrefix));
        WorkflowDescription workflowDescription = new WorkflowDescription("WGS" + testPrefix, billableEvents,
                CreateIssueRequest.Fields.Issuetype.Whole_Genome_Shotgun);
        BasicProjectPlan projectPlan = new BasicProjectPlan(project, "To test whole genome shotgun", workflowDescription);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            String barcode = "R" + testPrefix + rackPosition;

            String bspStock = "SM-" +  testPrefix + rackPosition;
            Sample passSample = new Sample();
            passSample.setBspSampleID(bspStock);
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(passSample,new BSPSample(bspStock + ".aliquot", projectPlan, null));
            mapBarcodeToTube.put(barcode,bspAliquot);

            twoDBarcodedTubeDAO.persist(bspAliquot);
        }
        twoDBarcodedTubeDAO.flush();
        twoDBarcodedTubeDAO.clear();

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

        LabEventTest.PreFlightJaxbBuilder preFlightJaxbBuilder = new LabEventTest.PreFlightJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                new ArrayList<String>(mapBarcodeToTube.keySet())).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : preFlightJaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        LabEventTest.ShearingJaxbBuilder shearingJaxbBuilder = new LabEventTest.ShearingJaxbBuilder(bettaLimsMessageFactory,
                new ArrayList<String>(mapBarcodeToTube.keySet()), testPrefix, preFlightJaxbBuilder.getRackBarcode()).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : shearingJaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        Map<String,StaticPlate> mapBarcodeToPlate = indexedPlateFactory.parseFile(
                new File(Thread.currentThread().getContextClassLoader().getResource("testdata/DuplexCOAforBroad.xlsx").getFile()),
                IndexedPlateFactory.TechnologiesAndParsers.ILLUMINA_SINGLE);
        StaticPlate indexPlate = mapBarcodeToPlate.values().iterator().next();
        if(staticPlateDAO.findByBarcode(indexPlate.getLabel()) == null) {
            staticPlateDAO.persist(indexPlate);
            staticPlateDAO.flush();
            staticPlateDAO.clear();
        }
        LabEventTest.LibraryConstructionJaxbBuilder libraryConstructionJaxbBuilder = new LabEventTest.LibraryConstructionJaxbBuilder(
                bettaLimsMessageFactory, testPrefix, shearingJaxbBuilder.getShearCleanPlateBarcode(), indexPlate.getLabel()).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        LabEventTest.HybridSelectionJaxbBuilder hybridSelectionJaxbBuilder = new LabEventTest.HybridSelectionJaxbBuilder(bettaLimsMessageFactory,
                testPrefix, libraryConstructionJaxbBuilder.getPondRegRackBarcode(),
                libraryConstructionJaxbBuilder.getPondRegTubeBarcodes()).invoke();
        twoDBarcodedTubeDAO.persist(LabEventTest.buildBaitTube(hybridSelectionJaxbBuilder.getBaitTubeBarcode()));
        twoDBarcodedTubeDAO.flush();
        twoDBarcodedTubeDAO.clear();
        for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        LabEventTest.QtpJaxbBuilder qtpJaxbBuilder = new LabEventTest.QtpJaxbBuilder(bettaLimsMessageFactory, testPrefix,
                hybridSelectionJaxbBuilder.getNormCatchBarcodes(), hybridSelectionJaxbBuilder.getNormCatchRackBarcode()).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }
//        Controller.stopCPURecording();

        String runName = "TestRun" + testPrefix;
        IlluminaSequencingRun illuminaSequencingRun = new IlluminaSequencingRun(
                illuminaFlowcellDao.findByBarcode(qtpJaxbBuilder.getFlowcellBarcode()), runName, runName, "SL-HAL", null, false);
        illuminaSequencingRunDao.persist(illuminaSequencingRun);
        illuminaSequencingRunDao.flush();
        System.out.println("Test run name " + runName);
    }

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testHttp(@ArquillianResource URL baseUrl) {
        File inboxDirectory = new File("C:/Temp/seq/lims/bettalims/production/inbox");
        List<String> dayDirectoryNames =  Arrays.asList(inboxDirectory.list());
        Collections.sort(dayDirectoryNames);
        for (String dayDirectoryName : dayDirectoryNames) {
            File dayDirectory = new File(inboxDirectory, dayDirectoryName);
            List<String> messageFileNames =  Arrays.asList(dayDirectory.list());
            Collections.sort(messageFileNames);
            for (String messageFileName : messageFileNames) {
                String response = null;
                try {
//                    String message = FileUtils.readFileToString(new File(dayDirectory, messageFileName));
//                    if(message.contains("PreSelectionPool")) {
                        response = Client.create().resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
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

    @Test(enabled = false, groups = EXTERNAL_INTEGRATION, dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testSingleFile(@ArquillianResource URL baseUrl) {
        String response = null;
        try {
            response = Client.create().resource(baseUrl.toExternalForm() + "rest/bettalimsmessage")
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .accept(MediaType.APPLICATION_XML)
                    .entity(new File("c:/Temp/sequel/messages/inbox/20120522/20120522_093905183.xml"))
                    .post(String.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println(response);
    }

}
