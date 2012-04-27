package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.BettaLimsMessageFactory;
import org.broadinstitute.sequel.bettalims.jaxb.PlateTransferEventType;
import org.broadinstitute.sequel.control.dao.person.PersonDAO;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.control.workflow.WorkflowParser;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.FIFOLabWorkQueue;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueName;
import org.broadinstitute.sequel.entity.queue.LcSetParameters;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheetImpl;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;


import static org.testng.Assert.*;

public class LabWorkQueueWorkflowTest {

    @Test
    public void test_root_plan() {
        doIt(false);
    }

    @Test
    public void test_plan_override() {
        doIt(true);
    }

    public void doIt(boolean useOverride) {

        WorkflowResolver wfResolver = new WorkflowResolver();
        int numSamples = 10;

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        Project rootProject = new BasicProject("Testing Root", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        Project overrideProject = new BasicProject("LabEventTesting", new JiraTicket(new DummyJiraService(),"TP-1","1"));

        WorkflowDescription workflow = new WorkflowDescription(WorkflowResolver.TEST_WORKFLOW_1,
                                                                          billableEvents,
                                                                          CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        ProjectPlan rootPlan = new ProjectPlan(rootProject,"The root plan", workflow);

        WorkflowParser workflowParser = new WorkflowParser(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(WorkflowResolver.TEST_WORKFLOW_1));
        workflow.setMapNameToTransitionList(workflowParser.getMapNameToTransitionList());
        workflow.setStartState(workflowParser.getStartState());


        ProjectPlan planOverride = new ProjectPlan(overrideProject,"Override tech dev plan", workflow);

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

        assertTrue(labWorkQueue.isEmpty());

        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            if (useOverride) {
                labWorkQueue.add(tube,originalParameters,rootPlan.getWorkflowDescription(),planOverride);
            }
            else {
                labWorkQueue.add(tube,originalParameters,rootPlan.getWorkflowDescription(),null);
            }
        }

        assertFalse(labWorkQueue.isEmpty());

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setPersonDAO(new PersonDAO());
        LabEventHandler labEventHandler = new LabEventHandler();

        List<String> errors = workflow.validate(new ArrayList<LabVessel>(mapBarcodeToTube.values()), "Start");
        Assert.assertEquals(errors, new ArrayList<String>(), "Workflow errors");
        String shearPlateBarcode = "ShearPlate";
        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "ShearingTransfer", "SomeRackBarcode", new ArrayList<String>(mapBarcodeToTube.keySet()), shearPlateBarcode);
        // for each vessel, get most recent event, check whether it's a predecessor to the proposed event
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);
        labEventHandler.processEvent(shearingTransferEventEntity, workflow);

        LabVessel outputPlate = shearingTransferEventEntity.getTargetLabVessels().iterator().next();

        for (SampleInstance sampleInstance : outputPlate.getSampleInstances()) {
            if (useOverride) {
                assertEquals(sampleInstance.getSingleProjectPlan(),planOverride);
            }
            else {
                assertEquals(sampleInstance.getSingleProjectPlan(),rootPlan);
            }
        }

        assertTrue(labWorkQueue.isEmpty());




    }
}
