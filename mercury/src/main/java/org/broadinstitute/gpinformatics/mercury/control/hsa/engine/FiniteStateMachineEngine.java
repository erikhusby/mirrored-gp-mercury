package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.StateManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.TaskManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;

import javax.ejb.TransactionManagement;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static javax.ejb.TransactionManagementType.BEAN;

@RequestScoped
@TransactionManagement(BEAN)
public class FiniteStateMachineEngine implements Serializable {
    private static final Log log = LogFactory.getLog(FiniteStateMachineEngine.class);

    @Inject
    private SchedulerContext context;

    @Inject
    private TaskManager taskManager;

    @Inject
    private StateManager stateManager;

    @Inject
    private StateMachineDao stateMachineDao;

    public FiniteStateMachineEngine() {
    }

    public FiniteStateMachineEngine(SchedulerContext context) {
        this.context = context;
    }

    public boolean resumeMachine(FiniteStateMachine stateMachine) {

        if (stateMachine.getActiveStates().isEmpty()) {
            throw new RuntimeException("No active states for " + stateMachine);
        }

        try {
            executeProcessDaoFree(stateMachine);
            stateMachineDao.persist(stateMachine);
            stateMachineDao.flush();
            return true;
        } catch (Exception e) {
            log.error("Error occurred when resuming state machine " + stateMachine, e);
        }

        return false;
    }

    @DaoFree
    public void executeProcessDaoFree(FiniteStateMachine stateMachine) {
        for (State state : stateMachine.getActiveStates()) {

            if (state.isStartState() && stateMachine.getDateStarted() == null) {
                stateMachine.setDateStarted(new Date());
            }

            if (state.isStateOnEnter()) {
                if (stateManager.handleOnEnter(state)) {
                    state.setStartTime(new Date());
                    for (Task task : state.getTasks()) {
                        fireEventAndCheckStatus(task);
                    }
                }
            }

            log.debug("Checking transitions from " + state);
            for (Task task: state.getActiveTasks()) {
                taskManager.checkTaskStatus(task, context);
            }

            if (state.isMainTasksComplete()) {
                if (state.getExitTask().isPresent()) {
                    Task exitTask = state.getExitTask().get();
                    if (state.isExitTaskPending()) {
                        try {
                            taskManager.fireEvent(exitTask, context);
                        } catch (Exception e) {
                            log.error("Failed to fire exit task " + exitTask.getTaskName(), e);
                            exitTask.setStatus(Status.SUSPENDED);
                        }
                    }
                    if (exitTask.getStatus() == Status.COMPLETE) {
                        exitTask.setEndTime(new Date());
                    }
                }
            }

            if (state.isComplete()) {
                incrementStateMachine(stateMachine, state, new MessageCollection());
            } else {
                for (Task task: state.getTasksFromStatusList(Arrays.asList(Status.RETRY, Status.REQUEUE))) {
                    try {
                        if (task.getStatus() == Status.REQUEUE) {
                            if (!stateManager.handleOnEnter(task.getState())) {
                                log.error("Failed to re-enter state");
                                continue;
                            }
                        }
                        fireEventAndCheckStatus(task);
                    } catch (Exception e) {
                        log.error("Error retrying failed task " + task.getTaskName(), e);
                        task.setStatus(Status.SUSPENDED);
                    }
                }
            }
        }

        if (stateMachine.isComplete()) {
            stateMachine.setStatus(Status.COMPLETE);
            stateMachine.setDateCompleted(new Date());
        }
    }

    public void incrementStateMachine(FiniteStateMachine stateMachine, MessageCollection messageCollection) {
        if (stateMachine.getActiveStates().isEmpty()) {
            throw new RuntimeException("No active states for " + stateMachine);
        }

        try {
            for (State state: stateMachine.getActiveStates()) {
                incrementStateMachine(stateMachine, state, messageCollection);
            }
            stateMachineDao.persist(stateMachine);
            stateMachineDao.flush();
        } catch (Exception e) {
            String errMsg = "Error occurred when resuming state machine " + stateMachine;
            log.error(errMsg, e);
            messageCollection.addError(errMsg);
        }
    }

    private void incrementStateMachine(FiniteStateMachine stateMachine, State state, MessageCollection messageCollection) {
        if (stateManager.handleOnExit(state)) {
            state.setAlive(false);
            state.setEndTime(new Date());
            List<Transition> transitionsFromState = stateMachine.getTransitionsFromState(state);
            for (Transition transition : transitionsFromState) {
                State toState = transition.getToState();
                toState.setAlive(true);
                if (stateManager.handleOnEnter(toState)) {
                    toState.setStartTime(new Date());
                    for (Task task : toState.getTasks()) {
                        fireEventAndCheckStatus(task);
                    }
                } else {
                    messageCollection.addError("Failed to enter state " + toState.getStateId());
                }
            }
        } else {
            messageCollection.addError("Failed to exit state " + state.getStateId());
        }
    }

    private void fireEventAndCheckStatus(Task task){
        try {
            taskManager.fireEvent(task, context);
            taskManager.checkTaskStatus(task, context);
        } catch (Exception e) {
            log.error("Error firing next task " + task.getTaskName(), e);
            task.setStatus(Status.SUSPENDED);
        }
    }

    // For tests only
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void setContext(SchedulerContext context) {
        this.context = context;
    }

    public SchedulerContext getContext() {
        return context;
    }

    public void setStateManager(StateManager stateManager) {
        this.stateManager = stateManager;
    }
}
