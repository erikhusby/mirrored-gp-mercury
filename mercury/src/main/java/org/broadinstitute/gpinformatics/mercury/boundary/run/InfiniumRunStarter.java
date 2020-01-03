package org.broadinstitute.gpinformatics.mercury.boundary.run;

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
import javax.transaction.SystemException;

import java.util.Date;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Singleton to configure and schedule the timer for Infinium run starting activities.
 */
@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class InfiniumRunStarter {
    private static final Log log = LogFactory.getLog(InfiniumRunStarter.class);

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
    private InfiniumRunFinder infiniumRunFinder;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @PostConstruct
    public void initialize() {
        ScheduleExpression expression = new ScheduleExpression();
        expression.minute("*/" + timerPeriod).hour("*");
        timerService.createCalendarTimer(expression, new TimerConfig("Infinium run timer", false));
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
        if (isEnabled()) {
            // Skips retries, indicated by a repeated nextTimeout value.
            Date nextTimeout = timer.getNextTimeout();
            if (nextTimeout.after(previousNextTimeout)) {
                previousNextTimeout = nextTimeout;
                sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                    @Override
                    public void apply() {
                        try {
                            infiniumRunFinder.find();
                        } catch (SystemException e) {
                            log.error("Error finding infinium runs", e);
                        }
                    }
                });
            } else {
                log.trace("Skipping Infinium Starter timer retry");
            }
        }
    }

    /**
     * Check Mercury configuration in the YAML file and see if the Infinium Starter system is enabled for this
     * environment.  If it is not, then the configuration will be null.
     *
     * @return true if it's an environment where the Infinium Starter should be run
     */
    private boolean isEnabled() {
        boolean useRunFinder = Boolean.getBoolean("useInfiniumRunFinder");
        if (!useRunFinder) {
            return false;
        }
        // Can't use @Inject for this object or we'll run into VFS protocol errors.
        InfiniumStarterConfig infiniumStarterConfig = (InfiniumStarterConfig) MercuryConfiguration.getInstance().
                getConfig(InfiniumStarterConfig.class, deployment);
        return AbstractConfig.isSupported(infiniumStarterConfig);

    }
}
