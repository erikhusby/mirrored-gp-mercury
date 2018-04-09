package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
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
import javax.inject.Inject;
import java.util.Date;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Singleton to configure and schedule the timer for daily activities, initially Infinium archiving.
 */
@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class InfiniumArchiveStarter {
    private static final Log log = LogFactory.getLog(InfiniumArchiveStarter.class);

    @Inject
    private Deployment deployment;

    private static Date previousNextTimeout = new Date(0);

    @Resource
    private TimerService timerService;

    @Inject
    private InfiniumArchiver infiniumArchiver;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @PostConstruct
    public void initialize() {
        ScheduleExpression expression = new ScheduleExpression();
        if (deployment.equals(Deployment.PROD)) {
            // 10pm
            expression.hour("22");
        } else {
            // Every 5 minutes
            expression.minute("*/5").hour("*");
        }
        timerService.createCalendarTimer(expression, new TimerConfig("Infinium run timer", false));
    }

    /**
     * This method does all the work -- it gets called at every interval defined by the timerPeriod.  The check for the
     * isEnabled() is done here instead of the initialize() simply because YAML needs to get a servlet or file
     * protocol handler.
     *
     * @param timer the defined {@Timer}
     */
    @Timeout
    void findRuns(Timer timer) {
        if (isEnabled()) {
            // Skips retries, indicated by a repeated nextTimeout value.
            Date nextTimeout = timer.getNextTimeout();
            if (nextTimeout.after(previousNextTimeout)) {
                previousNextTimeout = nextTimeout;
                sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                    @Override
                    public void apply() {
                        infiniumArchiver.archive();
                    }
                });
            } else {
                log.trace("Skipping Infinium Archive timer retry");
            }
        }
    }

    /**
     * Check Mercury configuration in the YAML file and see if the Infinium Archiver system is enabled for this
     * environment.  If it is not, then the configuration will be null.
     *
     * @return true if it's an environment where the Infinium Starter should be run
     */
    private boolean isEnabled() {
        boolean useRunFinder = Boolean.getBoolean("useInfiniumArchiver");
        // Can't use @Inject for this object or we'll run into VFS protocol errors.
        InfiniumStarterConfig infiniumStarterConfig = (InfiniumStarterConfig) MercuryConfiguration.getInstance().
                getConfig(InfiniumStarterConfig.class, deployment);
        return useRunFinder && AbstractConfig.isSupported(infiniumStarterConfig);

    }
}
