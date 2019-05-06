package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;

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
import java.util.Date;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class StateMachineStarter {

    private static final Log log = LogFactory.getLog(StateMachineStarter.class);

    @Inject
    private Deployment deployment;

    /**
     * Interval in minutes for the timer to fire off.
     */
    private int timerPeriod = 5;

    private static Date previousNextTimeout = new Date(0);

    @Resource
    private TimerService timerService;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @Inject
    private StateMachineDao stateMachineDao;

    @Inject
    private FiniteStateMachineEngine engine;

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
        // Skips retries, indicated by a repeated nextTimeout value.
        Date nextTimeout = timer.getNextTimeout();
        if (nextTimeout.after(previousNextTimeout)) {
            previousNextTimeout = nextTimeout;
            sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                @Override
                public void apply() {
                    //TODO User bean for audit trail
                    for (FiniteStateMachine stateMachine: stateMachineDao.findByStatus(Status.RUNNING)) {
                        engine.resumeMachine(stateMachine);
                    }
                }
            });
        } else {
            log.trace("Skipping dragen process watcher timer retry");
        }
    }
}
