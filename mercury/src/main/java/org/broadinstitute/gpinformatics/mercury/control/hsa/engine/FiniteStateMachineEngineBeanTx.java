package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.StateManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.TaskManager;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import static javax.ejb.TransactionManagementType.BEAN;

/**
 * This variant of FiniteStateMachineEngine has bean-managed transactions, and is intended to be called from
 * a TimerService-driven bean, not from ActionBeans.
 */
@Stateful
@RequestScoped
@TransactionManagement(BEAN)
public class FiniteStateMachineEngineBeanTx {
    private static final Log log = LogFactory.getLog(FiniteStateMachineEngineBeanTx.class);

    @Inject
    private SchedulerContext context;

    @Inject
    private TaskManager taskManager;

    @Inject
    private StateManager stateManager;

    @Inject
    private StateMachineDao stateMachineDao;

    @Resource
    private EJBContext ejbContext;

    public void resumeMachine(FiniteStateMachine stateMachine) {

        if (stateMachine.getActiveStates().isEmpty()) {
            throw new RuntimeException("No active states for " + stateMachine);
        }

        try {
            ejbContext.getUserTransaction().begin();
            FiniteStateMachineEngine.executeProcessDaoFree(stateMachine, stateManager, taskManager, context);
            stateMachineDao.persist(stateMachine);
            stateMachineDao.flush();
            ejbContext.getUserTransaction().rollback();
        } catch (Exception e) {
            log.error("Error occurred when resuming state machine " + stateMachine, e);
        }
    }


    public void setContext(SchedulerContext context) {
        this.context = context;
    }
}
