package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.DragenAppContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SlurmController;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.transaction.SystemException;
import java.util.Arrays;
import java.util.Date;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class FiniteStateMachineStarter {

    private static final Log log = LogFactory.getLog(FiniteStateMachineStarter.class);

    /**
     * Interval in minutes for the timer to fire off.
     */
    private int timerPeriod = 2;

    private static Date previousNextTimeout = new Date(0);

    @Resource
    private TimerService timerService;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private UserBean userBean;

    @Inject
    private FiniteStateMachineEngine engine;

    @Inject
    private SlurmController slurmController;

    @PostConstruct
    public void initialize() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.minute("*/" + timerPeriod).hour("*");
        timerService.createCalendarTimer(expression, new TimerConfig("Dragen process run timer", false));
    }

    /**
     * This method does all the work -- it gets called at every interval defined by the timerPeriod.  The check for the
     * isEnabled() is done here instead of the initialize() is simply because YAML needs to get a servlet or file
     * protocol handler.
     *
     * @see {@link AbstractConfig}
     *
     * @param timer the defined {@Timer}
     */
    @Timeout
    void findRuns(Timer timer) {
        if (!isEnabled()) {
            return;
        }

        // Skips retries, indicated by a repeated nextTimeout value.
        Date nextTimeout = timer.getNextTimeout();
        if (nextTimeout.after(previousNextTimeout)) {
            previousNextTimeout = nextTimeout;
            sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                @Override
                public void apply() {
                    userBean.login("seqsystem");
                    SchedulerContext schedulerContext = new SchedulerContext(slurmController);

                    for (FiniteStateMachine stateMachine: stateMachineDao.findByStatuses(Arrays.asList(Status.RUNNING, Status.QUEUED))) {
                        engine.setContext(schedulerContext);
                        try {
                            engine.resumeMachine(stateMachine);
                        } catch (SystemException e) {
                            log.error("Error starting state machines", e);
                        }
                    }
                }
            });
        } else {
            log.trace("Skipping dragen process watcher timer retry");
        }
    }

    /**
     * Checks for system vairable useFiniteStateMachineStarter to see if its is enabled for this
     * environment.
     *
     * @return true if it's an environment where the finite state machine starter should be run
     */
    private boolean isEnabled() {
        return Boolean.getBoolean("useFiniteStateMachineStarter");
    }
}
