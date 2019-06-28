package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

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
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
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
@TransactionManagement(TransactionManagementType.BEAN)
public class ExtractTransformRunner {
    private static final Log log = LogFactory.getLog(ExtractTransform.class);

    @Inject
    private Deployment deployment;

    /**
     * Interval in minutes for the timer to fire off.
     */
    private int timerPeriod = 5;

    private static Date previousNextTimeout = new Date(0);

    // ETL must be enabled via (JMX immutable) system property at startup (System.getProperty())
    private boolean isETLEnabled = Boolean.getBoolean("enableETL");

    @Resource
    TimerService timerService;

    @Inject
    private ExtractTransform extractTransform;

    @PostConstruct
    public void initialize() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.minute("*/" + timerPeriod).hour("*");
        timerService.createCalendarTimer(expression, new TimerConfig("ETL timer", false));
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
    void scheduledEtl(Timer timer) {
        if (isEnabled()) {
            // Skips retries, indicated by a repeated nextTimeout value.
            Date nextTimeout = timer.getNextTimeout();
            if (nextTimeout.after(previousNextTimeout)) {
                previousNextTimeout = nextTimeout;
                extractTransform.initConfig();
                extractTransform.incrementalEtl("0", "0");
            } else {
                log.trace("Skipping ETL timer retry");
            }
        }
    }

    /**
     * Check Mercury configuration in the YAML file and startup environment and see if the Data Warehouse system is enabled for this
     * environment.  If it is not, then the configuration will be null.
     *
     * @return true if it's an environment where ETL should be run
     */
    private boolean isEnabled() {

        if( !isETLEnabled ) {
            return false;
        }

        // ETL must also be configured in mercury-config.yaml file
        EtlConfig etlConfig = (EtlConfig) MercuryConfiguration.getInstance().getConfig(EtlConfig.class, deployment);
        return AbstractConfig.isSupported(etlConfig);
    }
}