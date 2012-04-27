package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.BettaLimsMessageFactory;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.FIFOLabWorkQueue;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueName;
import org.broadinstitute.sequel.entity.queue.LcSetParameters;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class LabWorkQueueWorkflowTest {

    @Test
    public void test_lab_dequeue_and_workflow_start() {
        WorkflowResolver wfResolver = new WorkflowResolver();
        int numSamples = 10;

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project rootProject = new BasicProject("Testing Root", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        Project overrideProject = new BasicProject("LabEventTesting", new JiraTicket(new DummyJiraService(),"TP-1","1"));

        WorkflowDescription workflow = new WorkflowDescription(WorkflowResolver.TEST_WORKFLOW_1,
                                                                          billableEvents,
                                                                          CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        ProjectPlan rootPlan = new ProjectPlan(rootProject,"To test hybrid selection", workflow);


        ProjectPlan planOverride = new ProjectPlan(overrideProject,"To test hybrid selection", workflow);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            SampleSheetImpl sampleSheet = new SampleSheetImpl();
            sampleSheet.addStartingSample(new BSPSample("SM-" + rackPosition, rootPlan, null));
            String barcode = "R" + rackPosition;
            mapBarcodeToTube.put(barcode, new TwoDBarcodedTube(barcode, sampleSheet));
        }

        // add the samples to the queue, no project plan override
        FIFOLabWorkQueue<LcSetParameters> labWorkQueue = new FIFOLabWorkQueue<LcSetParameters>(LabWorkQueueName.LC,new DummyJiraService());
        LcSetParameters originalParameters = new LcSetParameters();

        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            labWorkQueue.add(tube,null,rootPlan.getWorkflowDescription(),null);
        }

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler();

        List<String> errors = workflow.validate(new ArrayList<LabVessel>(mapBarcodeToTube.values()), "Start");
        Assert.assertEquals(errors, new ArrayList<String>(), "Workflow errors");
        String shearPlateBarcode = "ShearPlate";
        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "Start", "SomeRackBarcode", new ArrayList<String>(mapBarcodeToTube.keySet()), shearPlateBarcode);
        // for each vessel, get most recent event, check whether it's a predecessor to the proposed event
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);
        labEventHandler.processEvent(shearingTransferEventEntity, null);




    }
}
