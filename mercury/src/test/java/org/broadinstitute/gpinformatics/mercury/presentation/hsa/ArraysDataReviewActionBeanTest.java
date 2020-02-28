package org.broadinstitute.gpinformatics.mercury.presentation.hsa;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DragenConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.PushIdatsToCloudTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.StateManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.TaskManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForIdatTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForInfiniumMetric;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.WaitForReviewTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.PushIdatsToCloudHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.WaitForIdatTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers.WaitForInfiniumMetricsTaskHandler;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineEngine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.engine.FiniteStateMachineFactory;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerControllerStub;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.CreateArraysStateMachineHandler;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ArrayPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.InfiniumEntityBuilder;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class ArraysDataReviewActionBeanTest extends BaseEventTest {

    private InfiniumStarterConfig infiniumConfig;

    @Test
    public void testCreateWorkflowAndValidate() throws IOException {
        super.setUp();
        expectedRouting = SystemOfRecord.System.MERCURY;

        ProductOrder productOrder = ProductOrderTestFactory.buildInfiniumProductOrder(94);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R",
                MercurySample.MetadataSource.MERCURY);

        LabBatch workflowBatch = new LabBatch("Infinium Batch",
                new HashSet<>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.INFINIUM);
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");

        TubeFormation daughterTubeFormation = daughterPlateTransfer(mapBarcodeToTube, workflowBatch);

        Map<String, BarcodedTube> mapBarcodeToDaughterTube = new HashMap<>();
        for (BarcodedTube barcodedTube : daughterTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToDaughterTube.put(barcodedTube.getLabel(), barcodedTube);
        }

        ArrayPlatingEntityBuilder arrayPlatingEntityBuilder =
                runArrayPlatingProcess(mapBarcodeToDaughterTube, "Infinium");

        InfiniumEntityBuilder infiniumEntityBuilder = runInfiniumProcess(
                arrayPlatingEntityBuilder.getArrayPlatingPlate(), "Infinium");

        CreateArraysStateMachineHandler handler = new CreateArraysStateMachineHandler();
        FiniteStateMachineFactory factory = new FiniteStateMachineFactory();
        StateMachineDao stateMachineDao = mock(StateMachineDao.class);
        when(stateMachineDao.findByIdentifier(anyString())).thenReturn(null);
        ArgumentCaptor<FiniteStateMachine> valueCapture = ArgumentCaptor.forClass(FiniteStateMachine.class);
        doNothing().when(stateMachineDao).persist(valueCapture.capture());
        factory.setStateMachineDao(stateMachineDao);
        handler.setFiniteStateMachineFactory(factory);
        handler.handleEvent(infiniumEntityBuilder.getxStainEvents().iterator().next(), null);

        List<FiniteStateMachine> finiteStateMachines = valueCapture.getAllValues();
        Assert.assertNotNull(finiteStateMachines);
        Assert.assertEquals(finiteStateMachines.size(), 0);

        // Add AoU metadata and retry
        for (BarcodedTube barcodedTube: mapBarcodeToTube.values()) {
            for (MercurySample mercurySample: barcodedTube.getMercurySamples()) {
                Metadata metadata = new Metadata(Metadata.Key.PRODUCT_TYPE, MayoManifestEjb.AOU_ARRAY);
                mercurySample.addMetadata(Collections.singleton(metadata));
            }
        }

        handler.handleEvent(infiniumEntityBuilder.getxStainEvents().iterator().next(), null);
        finiteStateMachines = valueCapture.getAllValues();
        Assert.assertNotNull(finiteStateMachines);
        Assert.assertEquals(finiteStateMachines.size(), 24);

        FiniteStateMachine finiteStateMachine = finiteStateMachines.iterator().next();
        Assert.assertEquals(finiteStateMachine.getActiveStates().size(), 1);
        State idatState = finiteStateMachine.getActiveStates().iterator().next();
        Set<WaitForIdatTask> waitForIdatTasks = idatState.getTasksOfType(WaitForIdatTask.class);
        Assert.assertEquals(waitForIdatTasks.size(), 1);

        List<WaitForReviewTask> waitForReviewTasks = finiteStateMachine.fetchAllTasksOfType(WaitForReviewTask.class);
        Assert.assertEquals(waitForReviewTasks.size(), 1);
        WaitForReviewTask waitForReviewTask = waitForReviewTasks.iterator().next();

        List<PushIdatsToCloudTask> pushTasks = finiteStateMachine.fetchAllTasksOfType(PushIdatsToCloudTask.class);
        int numIdatsToPush = 2;
        Assert.assertEquals(pushTasks.size(), numIdatsToPush);

        DragenConfig dragenConfig = new DragenConfig(Deployment.DEV);
        infiniumConfig = new InfiniumStarterConfig(Deployment.DEV);
        File infiniumDataPath = Files.createTempDirectory("InfiniumDataPath").toFile();
        infiniumConfig.setDataPath(infiniumDataPath.getPath());
        SchedulerContext schedulerContext = new SchedulerContext(new SchedulerControllerStub());
        FiniteStateMachineEngine engine = new FiniteStateMachineEngine(schedulerContext);
        StateManager stateHandler = mock(StateManager.class);
        when(stateHandler.handleOnEnter(any(State.class))).thenReturn(true);
        when(stateHandler.handleOnExit(any(State.class))).thenReturn(true);
        engine.setStateManager(stateHandler);

        TaskManager taskManager = new TaskManager();
        WaitForIdatTaskHandler waitForIdatTaskHandler = new WaitForIdatTaskHandler();
        waitForIdatTaskHandler.setInfiniumStarterConfig(infiniumConfig);
        taskManager.setWaitForIdatTaskHandler(waitForIdatTaskHandler);

        PushIdatsToCloudHandler pushIdatsToCloudHandler = new PushIdatsToCloudHandler();
        pushIdatsToCloudHandler.setInfiniumStarterConfig(infiniumConfig);
        pushIdatsToCloudHandler.setDragenConfig(dragenConfig);
        taskManager.setPushIdatsToCloudHandler(pushIdatsToCloudHandler);

        WaitForInfiniumMetricsTaskHandler waitForInfiniumMetricsTaskHandler = new WaitForInfiniumMetricsTaskHandler();
        ArraysQcDao arraysQcDao = mock(ArraysQcDao.class);
        waitForInfiniumMetricsTaskHandler.setArraysQcDao(arraysQcDao);
        taskManager.setWaitForInfiniumMetricsTaskHandler(waitForInfiniumMetricsTaskHandler);

        engine.setTaskManager(taskManager);

        executeAndVerifyWaitForIdats(finiteStateMachine, engine, waitForIdatTasks.iterator().next());

        // Since Wait for is Complete, engine will autotrigger next state - push to cloud which should now be complete
        Iterator<PushIdatsToCloudTask> iterator = pushTasks.iterator();
        Assert.assertEquals(iterator.next().getStatus(), Status.COMPLETE);
        Assert.assertEquals(iterator.next().getStatus(), Status.COMPLETE);

        // Will be in the Wait For Metrics after next execution
        engine.executeProcessDaoFree(finiteStateMachine);
        List<State> activeStates = finiteStateMachine.getActiveStates();
        Assert.assertEquals(activeStates.size(), 1);
        State waitForMetrics = activeStates.iterator().next();
        WaitForInfiniumMetric waitForInfiniumMetric = waitForMetrics.getTasksOfType(WaitForInfiniumMetric.class).iterator().next();
        Assert.assertEquals(waitForInfiniumMetric.getStatus(), Status.RUNNING);

        LabVessel next = waitForInfiniumMetric.getState().getLabVessels().iterator().next();
        List<ArraysQc> arraysQcMetrics = new ArrayList<>();
        ArraysQc arraysQc = new ArraysQc();
        arraysQcMetrics.add(arraysQc);
        when(arraysQcDao.findByBarcodes(Arrays.asList(next.getLabel()))).thenReturn(arraysQcMetrics);
        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(waitForInfiniumMetric.getStatus(), Status.COMPLETE);

        // Will have fired off the Wait For Review Task, which puts Machine into 'Triage' to avoid computation until
        // human interaction
        Assert.assertEquals(waitForReviewTask.getStatus(), Status.RUNNING);
        Assert.assertEquals(finiteStateMachine.getStatus(), Status.TRIAGE);
    }

    private void executeAndVerifyWaitForIdats(FiniteStateMachine finiteStateMachine,
                                              FiniteStateMachineEngine engine,
                                              WaitForIdatTask task) throws IOException {
        // First execution the idats don't exist, will still be in a state of running
        Assert.assertEquals(task.getStatus(), Status.QUEUED);
        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(task.getStatus(), Status.RUNNING);

        LabVessel chipWell = task.getState().getLabVessels().iterator().next();
        String chipBarcode = ((PlateWell)chipWell).getPlate().getLabel();
        String vesselPosition = ((PlateWell) chipWell).getVesselPosition().name();
        String red = String.format("%s_%s_Red.idat", chipBarcode, vesselPosition);
        String green = String.format("%s_%s_Grn.idat", chipBarcode, vesselPosition);
        File dataFolder = new File(infiniumConfig.getDataPath());
        File chipFolder = new File(dataFolder, chipBarcode);
        chipFolder.mkdir();

        new File(chipFolder, red).createNewFile();
        new File(chipFolder, green).createNewFile();

        engine.executeProcessDaoFree(finiteStateMachine);
        Assert.assertEquals(task.getStatus(), Status.COMPLETE);
    }
}