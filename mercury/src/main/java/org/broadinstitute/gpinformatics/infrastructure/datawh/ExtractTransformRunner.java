package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.inject.Inject;
import java.util.Date;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Singleton to configure and schedule the timer for ETL Warehouse activities.  There is no data warehouse for CLIA, so
 * it is turned off for that environment.
 */
@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class ExtractTransformRunner {
    private static final Log log = LogFactory.getLog(ExtractTransform.class);

    /**
     * Interval in minutes for the timer to fire off.
     */
    private int timerPeriod = 5;

    private static Date previousNextTimeout = new Date(0);

    @Resource
    TimerService timerService;

    @Inject
    private ExtractTransform extractTransform;

    @PostConstruct
    public void initialize() {
        if (isEnabled()) {
            ScheduleExpression expression = new ScheduleExpression();
            expression.minute("*/" + timerPeriod).hour("*");
            timerService.createCalendarTimer(expression, new TimerConfig("ETL timer", false));
        }
    }

    @Timeout
    void scheduledEtl(Timer timer) {
        if (isEnabled()) {
            // Skips retries, indicated by a repeated nextTimeout value.
            Date nextTimeout = timer.getNextTimeout();
            if (nextTimeout.after(previousNextTimeout)) {
                previousNextTimeout = nextTimeout;
                extractTransform.initConfig();
                extractTransform.incrementalEtl("0", "0");
            } else {
                log.debug("Skipping ETL timer retry.");
            }
        }
    }

    /**
     * Iterate through a list of different reasons why the ETL should not be running.  Currently the only instance
     * where it should not run is for the CRSP deployment.
     *
     * @return true if it's an environment where ETL should be run
     */
    private boolean isEnabled() {
        if (Deployment.isCRSP) {
            return false;
        }

        return true;
    }
}