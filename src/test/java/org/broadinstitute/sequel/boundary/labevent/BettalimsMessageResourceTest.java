package org.broadinstitute.sequel.boundary.labevent;

//import com.jprofiler.api.agent.Controller;
import org.broadinstitute.sequel.BettaLimsMessageFactory;
import org.broadinstitute.sequel.LabEventTest;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.bettalims.jaxb.BettaLIMSMessage;
import org.broadinstitute.sequel.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.sequel.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.sequel.control.vessel.IndexedPlateFactory;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.run.IlluminaSequencingRun;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testProcessMessage() {
        String testPrefix = testPrefixDateFormat.format(new Date());
//        Controller.startCPURecording(true);

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project project = new BasicProject(testPrefix + "LabEventTesting", new JiraTicket(new DummyJiraService(),
                "TP-" + testPrefix, testPrefix));
        WorkflowDescription workflowDescription = new WorkflowDescription("WGS" + testPrefix, billableEvents,
                CreateIssueRequest.Fields.Issuetype.Whole_Genome_Shotgun);
        ProjectPlan projectPlan = new ProjectPlan(project, "To test whole genome shotgun", workflowDescription);
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= LabEventTest.NUM_POSITIONS_IN_RACK; rackPosition++) {
            SampleSheet sampleSheet = new SampleSheet();
            sampleSheet.addStartingSample(new BSPSample("SM-" + testPrefix + rackPosition, projectPlan, null));
            String barcode = "R" + testPrefix + rackPosition;
            TwoDBarcodedTube twoDBarcodedTube = new TwoDBarcodedTube(barcode, sampleSheet);
            mapBarcodeToTube.put(barcode, twoDBarcodedTube);
            twoDBarcodedTubeDAO.persist(twoDBarcodedTube);
        }
        twoDBarcodedTubeDAO.flush();
        twoDBarcodedTubeDAO.clear();

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();

        LabEventTest.PreFlightJaxb preFlightJaxb = new LabEventTest.PreFlightJaxb(bettaLimsMessageFactory, testPrefix,
                new ArrayList<String>(mapBarcodeToTube.keySet())).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : preFlightJaxb.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        LabEventTest.ShearingJaxb shearingJaxb = new LabEventTest.ShearingJaxb(bettaLimsMessageFactory,
                new ArrayList<String>(mapBarcodeToTube.keySet()), testPrefix, preFlightJaxb.getRackBarcode()).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : shearingJaxb.getMessageList()) {
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
        LabEventTest.LibraryConstructionJaxb libraryConstructionJaxb = new LabEventTest.LibraryConstructionJaxb(
                bettaLimsMessageFactory, testPrefix, shearingJaxb.getShearCleanPlateBarcode(), indexPlate.getLabel()).invoke();

        for (BettaLIMSMessage bettaLIMSMessage : libraryConstructionJaxb.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        LabEventTest.HybridSelectionJaxb hybridSelectionJaxb = new LabEventTest.HybridSelectionJaxb(bettaLimsMessageFactory,
                testPrefix, libraryConstructionJaxb.getPondRegRackBarcode(),
                libraryConstructionJaxb.getPondRegTubeBarcodes()).invoke();
        twoDBarcodedTubeDAO.persist(LabEventTest.buildBaitTube(hybridSelectionJaxb.getBaitTubeBarcode()));
        twoDBarcodedTubeDAO.flush();
        twoDBarcodedTubeDAO.clear();
        for (BettaLIMSMessage bettaLIMSMessage : hybridSelectionJaxb.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }

        LabEventTest.QtpJaxb qtpJaxb = new LabEventTest.QtpJaxb(bettaLimsMessageFactory, testPrefix,
                hybridSelectionJaxb.getNormCatchBarcodes(), hybridSelectionJaxb.getNormCatchRackBarcode()).invoke();
        for (BettaLIMSMessage bettaLIMSMessage : qtpJaxb.getMessageList()) {
            bettalimsMessageResource.processMessage(bettaLIMSMessage);
            twoDBarcodedTubeDAO.flush();
            twoDBarcodedTubeDAO.clear();
        }
//        Controller.stopCPURecording();

        String runName = "TestRun" + testPrefix;
        IlluminaSequencingRun illuminaSequencingRun = new IlluminaSequencingRun(
                illuminaFlowcellDao.findByBarcode(qtpJaxb.getFlowcellBarcode()), runName, runName, "SL-HAL", null, false);
        illuminaSequencingRunDao.persist(illuminaSequencingRun);
        illuminaSequencingRunDao.flush();
        System.out.println("Test run name " + runName);
    }
}
