package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.SystemException;
import java.io.Serializable;
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

    public void resumeMachine(FiniteStateMachine stateMachine) {

        if (stateMachine.getActiveStates().isEmpty()) {
            throw new RuntimeException("No active states for " + stateMachine);
        }

        try {
            executeProcessDaoFree(stateMachine);
            stateMachineDao.persist(stateMachine);
            stateMachineDao.flush();
        } catch (Exception e) {
            log.error("Error occurred when resuming state machine " + stateMachine, e);
        }
    }

    @DaoFree
    public void executeProcessDaoFree(FiniteStateMachine stateMachine) {
        for (State state : stateMachine.getActiveStates()) {

            if (state.isStartState() && stateMachine.getDateStarted() == null) {
                stateMachine.setDateStarted(new Date());
                if (stateManager.handleOnEnter(state)) {
                    state.setStartTime(new Date());
                    for (Task task : state.getTasks()) {
                        try {
                            taskManager.fireEvent(task, context);
                        } catch (Exception e) {
                            log.error("Error starting machine tasks " + task.getTaskName(), e);
                            task.setStatus(Status.SUSPENDED);
                        }
                    }
                }
            }

            if (state.isStateOnEnter()) {
                if (stateManager.handleOnEnter(state)) {
                    state.setStartTime(new Date());
                    for (Task task : state.getTasks()) {
                        try {
                            taskManager.fireEvent(task, context);
                        } catch (Exception e) {
                            log.error("Error firing next task " + task.getTaskName(), e);
                            task.setStatus(Status.SUSPENDED);
                        }
                    }
                }
            }

            log.debug("Checking transitions from " + state);
            for (Task task: state.getActiveTasks()) {
                Pair<Status, Date> statusDatePair = taskManager.checkTaskStatus(task, context);
                task.setStatus(statusDatePair.getLeft());
                if (task.getStatus() == Status.COMPLETE) {
                    task.setEndTime(statusDatePair.getRight());
                }
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
                                try {
                                    taskManager.fireEvent(task, context);
                                } catch (Exception e) {
                                    log.error("Error firing next task " + task.getTaskName(), e);
                                    task.setStatus(Status.SUSPENDED);
                                }
                            }
                        }
                    }
                }
            } else {
                for (Task task: state.getTasksWithStatus(Status.RETRY)) {
                    try {
                        taskManager.fireEvent(task, context);
                        if (task.getStatus() == Status.COMPLETE) {
                            task.setEndTime(new Date());
                        }
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

    // For tests only
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setContext(SchedulerContext context) {
        this.context = context;
    }

    public SchedulerContext getContext() {
        return context;
    }
}
