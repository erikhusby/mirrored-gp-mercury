package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.TaskManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Stateless
@Dependent
@TransactionManagement(value= TransactionManagementType.BEAN)
public class FiniteStateMachineEngine {
    private static final Log log = LogFactory.getLog(FiniteStateMachineEngine.class);

    @Inject
    private SchedulerContext context;

    @Inject
    private TaskManager taskManager;

    @Inject
    private StateMachineDao stateMachineDao;

    @Resource
    private EJBContext ejbContext;

    private static final AtomicBoolean busy = new AtomicBoolean(false);

    public FiniteStateMachineEngine() {
    }

    public FiniteStateMachineEngine(SchedulerContext context) {
        this.context = context;
    }

    public void resumeMachine(FiniteStateMachine stateMachine) throws SystemException {

        if (!busy.compareAndSet(false, true)) {
            return;
        }
        if (stateMachine.getActiveStates().isEmpty()) {
            throw new RuntimeException("No active states for " + stateMachine);
        }

        UserTransaction utx = ejbContext.getUserTransaction();
        try {
            utx.begin();
            executeProcess(stateMachine);
            utx.commit();
        } catch (Exception e) {
            utx.rollback();
            log.error("Error occured when resuming state machine " + stateMachine, e);
        } finally {
            busy.set(false);
        }
    }

    public void executeProcess(FiniteStateMachine stateMachine) {
        executeProcessDaoFree(stateMachine);
//        stateMachineDao.persist(stateMachine);
//        stateMachineDao.flush();
    }

    @DaoFree
    public void executeProcessDaoFree(FiniteStateMachine stateMachine) {
        for (State state : stateMachine.getActiveStates()) {

            if (state.isStartState() && stateMachine.getDateStarted() == null) {
                stateMachine.setDateStarted(new Date());
                state.OnEnter();
                for (Task task: state.getTasks()) {
                    try {
                        taskManager.fireEvent(task, context);
                    } catch (Exception e) {
                        log.error("Error starting machine tasks " + task.getTaskName(), e);
                        task.setStatus(Status.SUSPENDED);
                    }
                }
            }

            log.debug("Checking transitions from " + state);
            for (Task task: state.getActiveTasks()) {
                Status taskStatus = taskManager.checkTaskStatus(task, context);
                task.setStatus(taskStatus);
                if (taskStatus == Status.COMPLETE) {
                    task.setEndTime(new Date());
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
                state.setAlive(false);
                state.setEndTime(new Date());
                List<Transition> transitionsFromState = stateMachine.getTransitionsFromState(state);
                for (Transition transition : transitionsFromState) {
                    State toState = transition.getToState();
                    toState.setAlive(true);
                    toState.setStartTime(new Date());
                    toState.OnEnter();
                    for (Task task: toState.getTasks()) {
                        try {
                            taskManager.fireEvent(task, context);
                        } catch (Exception e) {
                            log.error("Error firing next task " + task.getTaskName(), e);
                            task.setStatus(Status.SUSPENDED);
                        }
                    }
                }
            } else {
                for (Task task: state.getTasksWithStatus(Status.RETRY)) {
                    try {
                        taskManager.fireEvent(task, context);
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
