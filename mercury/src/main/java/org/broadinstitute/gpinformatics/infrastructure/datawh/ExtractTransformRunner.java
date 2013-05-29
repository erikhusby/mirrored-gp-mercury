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
    private static final Log logger = LogFactory.getLog(ExtractTransform.class);
    private int timerMinutes = 5;
    private static Date previousNextTimeout = new Date(0);

    @Resource
    TimerService timerService;

    @Inject
    private ExtractTransform extractTransform;

    @PostConstruct
    public void initialize() {
        if (!Deployment.isCRSP) {
            ScheduleExpression expression = new ScheduleExpression();
            expression.minute("*/" + timerMinutes).hour("*");
            timerService.createCalendarTimer(expression, new TimerConfig("ETL timer", false));
        }
    }

    @Timeout
    void scheduledEtl(Timer timer) {
        if (!Deployment.isCRSP) {
            // Skips retries, indicated by a repeated nextTimeout value.
            Date nextTimeout = timer.getNextTimeout();
            if (nextTimeout.after(previousNextTimeout)) {
                previousNextTimeout = nextTimeout;
                extractTransform.initConfig();
                extractTransform.incrementalEtl("0", "0");
            } else {
                logger.debug("Skipping ETL timer retry.");
            }
        }
    }

}