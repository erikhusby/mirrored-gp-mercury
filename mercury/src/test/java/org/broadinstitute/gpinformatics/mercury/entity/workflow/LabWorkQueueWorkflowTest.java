package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.WorkQueueDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowParser;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventName;
import org.broadinstitute.gpinformatics.mercury.entity.project.*;
import org.broadinstitute.gpinformatics.mercury.entity.queue.FIFOLabWorkQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueueName;
import org.broadinstitute.gpinformatics.mercury.entity.queue.LcSetParameters;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.easymock.EasyMock;
import org.testng.annotations.Test;

import java.util.*;


import static org.testng.Assert.*;

public class LabWorkQueueWorkflowTest {

    @Test(enabled =  false)
    public void test_root_plan() {
        doIt(false);
    }

    @Test(enabled = false)
    public void test_plan_override() {
        doIt(true);
    }

    public void doIt(boolean useOverride) {

        WorkflowResolver wfResolver = new WorkflowResolver();
        int numSamples = 10;

        Map<LabEventName,PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
//        Project rootProject = new BasicProject("Testing Root", new JiraTicket(new JiraServiceStub(),"TP-0","0"));
//        Project overrideProject = new BasicProject("LabEventTesting", new JiraTicket(new JiraServiceStub(),"TP-1","1"));

        WorkflowDescription workflow = new WorkflowDescription(WorkflowResolver.TEST_WORKFLOW_1,
                                                                          null,
                                                                          CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
//        BasicProjectPlan rootPlan = new BasicProjectPlan(rootProject,"The root plan", workflow);

        WorkflowParser workflowParser = new WorkflowParser(
                Thread.currentThread().getContextClassLoader().getResourceAsStream(WorkflowResolver.TEST_WORKFLOW_1));
        workflow.setMapNameToTransitionList(workflowParser.getMapNameToTransitionList());
        workflow.setStartState(workflowParser.getStartState());


//        BasicProjectPlan planOverride = new BasicProjectPlan(overrideProject,"Override tech dev plan", workflow);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        for(int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            String barcode = "R" + rackPosition;

            String bspStock = "SM-" + rackPosition;
            BSPSampleAuthorityTwoDTube bspAliquot = new BSPSampleAuthorityTwoDTube(new BSPStartingSample(bspStock + ".aliquot", /*rootPlan, */null));
            mapBarcodeToTube.put(barcode,bspAliquot);

        }

        // add the samples to the queue, no project plan override
        final FIFOLabWorkQueue<LcSetParameters> labWorkQueue = new FIFOLabWorkQueue<LcSetParameters>(LabWorkQueueName.LC,new JiraServiceStub());
        LcSetParameters originalParameters = new LcSetParameters();

        assertTrue(labWorkQueue.isEmpty());

        for (TwoDBarcodedTube tube : mapBarcodeToTube.values()) {
            if (useOverride) {
                labWorkQueue.add(tube,originalParameters, workflow);
            }
            else {
                labWorkQueue.add(tube,originalParameters,workflow);
            }
        }

        assertFalse(labWorkQueue.isEmpty());

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        final LabEventFactory labEventFactory = new LabEventFactory();
        labEventFactory.setLabEventRefDataFetcher(new LabEventFactory.LabEventRefDataFetcher() {
            @Override
            public Person getOperator(String userId) {
                return new Person(userId);
            }

            @Override
            public LabBatch getLabBatch(String labBatchName) {
                return null;
            }
        });
        final LabEventHandler labEventHandler = new LabEventHandler(createMockWorkQueueDAO(labWorkQueue));

        String shearPlateBarcode = "ShearPlate";
        PlateTransferEventType shearingTransferEventJaxb = bettaLimsMessageFactory.buildRackToPlate(
                "ShearingTransfer", "SomeRackBarcode", new ArrayList<String>(mapBarcodeToTube.keySet()), shearPlateBarcode);
        // for each vessel, get most recent event, check whether it's a predecessor to the proposed event
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingTransferEventJaxb, mapBarcodeToTube, null);
        labEventHandler.processEvent(shearingTransferEventEntity);

        LabVessel outputPlate = shearingTransferEventEntity.getTargetLabVessels().iterator().next();

        for (SampleInstance sampleInstance : outputPlate.getSampleInstances()) {
            if (useOverride) {
//                assertEquals(sampleInstance.getSingleProjectPlan(),planOverride);
            }
            else {
//                assertEquals(sampleInstance.getSingleProjectPlan(),rootPlan);
            }
        }

        assertTrue(labWorkQueue.isEmpty());

        // PostShearingTransferCleanup
        final StaticPlate shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();

        // now toggle the project plan again, just for this plate.
        if (useOverride) {
            // if we've been using the override, now we'll skip it.  the result should
            // be that we still pickup the override plan because the override
            // is a total reset from the event on down in the transfer graph
            labWorkQueue.add(shearingPlate,originalParameters,workflow);

            String shearCleanPlateBarcode = "ShearCleanPlate";
            PlateTransferEventType postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
            LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    postShearingTransferCleanupEventJaxb, shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity);

            assertTrue(labWorkQueue.isEmpty());

            StaticPlate shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();

            for (SampleInstance sampleInstance : shearingCleanupPlate.getSampleInstances()) {
//                assertEquals(sampleInstance.getSingleProjectPlan(),planOverride);
            }

            // now we'll use the root plan again, this time as an override
            labWorkQueue.add(shearingPlate,originalParameters,workflow);

            shearCleanPlateBarcode = "ShearCleanPlate2";
            postShearingTransferCleanupEventJaxb = bettaLimsMessageFactory.buildPlateToPlate(
                    "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
            postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                    postShearingTransferCleanupEventJaxb, shearingPlate, null);
            labEventHandler.processEvent(postShearingTransferCleanupEntity);

            assertTrue(labWorkQueue.isEmpty());
            shearingCleanupPlate = (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();

            for (SampleInstance sampleInstance : shearingCleanupPlate.getSampleInstances()) {
//               assertEquals(sampleInstance.getSingleProjectPlan(),rootPlan);
            }

        }
    }

    private WorkQueueDAO createMockWorkQueueDAO(LabWorkQueue workQueue) {
        Set<LabWorkQueue> workQueues = new HashSet<LabWorkQueue>();
        workQueues.add(workQueue);

        WorkQueueDAO workQueueDAO = EasyMock.createMock(WorkQueueDAO.class);

        EasyMock.expect(workQueueDAO.getPendingQueues(
                (LabVessel)EasyMock.anyObject(),
                (WorkflowDescription)EasyMock.anyObject()
                )).andReturn(workQueues).atLeastOnce();

        EasyMock.replay(workQueueDAO);
        return workQueueDAO;
    }
}
