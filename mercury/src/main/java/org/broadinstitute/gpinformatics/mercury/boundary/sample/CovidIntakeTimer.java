package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

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
public class CovidIntakeTimer {
    @Inject
    private Deployment deployment;
    @Inject
    private SessionContextUtility sessionContextUtility;
    @Inject
    private CovidIntakeEjb covidIntakeEjb;
    @Resource
    private TimerService timerService;

    private static final Log log = LogFactory.getLog(CovidIntakeTimer.class);
    private static Date previousNextTimeout = new Date(0);
    private final String timerName = "CovidIntake timer";

    /**
     * Interval in minutes for the timer to fire off.
     */
    private int timerPeriod = 1;

    /** CDI constructor. */
    @SuppressWarnings("UnusedDeclaration")
    public CovidIntakeTimer() {}

    @PostConstruct
    public void postConstruct() {
        // Sets up the period timer.
        ScheduleExpression expression = new ScheduleExpression();
        expression.minute("*/" + timerPeriod).hour("*");
        timerService.createCalendarTimer(expression, new TimerConfig(timerName, false));
    }

    @Timeout
    void timeout(Timer timer) {
        // Skips retries, indicated by a repeated nextTimeout value.
        Date nextTimeout = timer.getNextTimeout();
        if (nextTimeout.after(previousNextTimeout)) {
            previousNextTimeout = nextTimeout;
            sessionContextUtility.executeInContext(() -> {
                try {
                    covidIntakeEjb.pollAndAccession();
                } catch (Exception e) {
                    log.error("Error in Covid intake.", e);
                }
            });
        } else {
            log.trace("Skipping retry of " + timerName);
        }
    }
}