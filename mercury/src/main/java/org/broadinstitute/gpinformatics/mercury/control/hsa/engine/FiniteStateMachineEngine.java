package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenAppContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.TaskManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Transition;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Stateless
@Dependent
@TransactionManagement(value= TransactionManagementType.BEAN)
public class FiniteStateMachineEngine {
    private static final Log log = LogFactory.getLog(FiniteStateMachineEngine.class);

    @Inject
    private DragenAppContext context;

    @Inject
    private TaskManager taskManager;

    @Inject
    private StateMachineDao stateMachineDao;

    private static final AtomicBoolean busy = new AtomicBoolean(false);

    public FiniteStateMachineEngine() {
    }

    public FiniteStateMachineEngine(DragenAppContext context) {
        this.context = context;
    }

    public void resumeMachine(FiniteStateMachine stateMachine) {

        if (!busy.compareAndSet(false, true)) {
            return;
        }

        try {
            if (stateMachine.getActiveStates().isEmpty()) {
                throw new RuntimeException("No active states for " + stateMachine);
            }

            executeProcess(stateMachine);
        } finally {
            busy.set(false);
        }
    }

    public void executeProcess(FiniteStateMachine stateMachine) {
        executeProcessDaoFree(stateMachine);
        stateMachineDao.persist(stateMachine);
        stateMachineDao.flush();
    }

    @DaoFree
    public void executeProcessDaoFree(FiniteStateMachine stateMachine) {
        for (State state : stateMachine.getActiveStates()) {

            if (state.isStartState() && stateMachine.getDateStarted() == null) {
                stateMachine.setDateStarted(new Date());
            }

            log.debug("Checking transitions from " + state);
            if (taskManager.isTaskComplete(state.getTask())) {
                List<Transition> transitionsFromState = stateMachine.getTransitionsFromState(state);
                for (Transition transition : transitionsFromState) {
                    log.info("Processing Transition " + transition);
                    State toState = transition.getToState();
                    taskManager.fireEvent(toState.getTask(), context);
                    toState.setAlive(true);
                    toState.setStartTime(new Date());
                }
                state.setAlive(false);
                state.setEndTime(new Date());
            }
        }

        if (stateMachine.isComplete()) {
            stateMachine.setStatus(Status.COMPLETE);
        }
    }

    // For tests only
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public void setContext(DragenAppContext context) {
        this.context = context;
    }

    public DragenAppContext getContext() {
        return context;
    }
}
