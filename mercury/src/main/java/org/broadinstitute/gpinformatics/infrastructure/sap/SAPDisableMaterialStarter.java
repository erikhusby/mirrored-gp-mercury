package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.control.run.InfiniumArchiver;

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

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Singleton to configure and schedule the timer for updating discontinued Products in SAP
 */
@Singleton
@Startup
@ConcurrencyManagement(BEAN)
public class SAPDisableMaterialStarter {

    private Log log = LogFactory.getLog(SAPDisableMaterialStarter.class);

    @Inject
    private Deployment deployment;

    @Resource
    private TimerService timerService;

    @Inject
    private InfiniumArchiver infiniumArchiver;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @Inject
    ProductEjb productEjb;

    /**
     * This method will do the intended work.  It delegates to ProductEjb to do the heavy lifting of disabling the
     * product
     * @param timer
     */
    @Timeout
    public void disableProducts(Timer timer) {
        productEjb.adjustMaterialStatusForToday();
    }

    /**
     * Sets up the Timer to run at scheduled intervals.  When it wakes up, the disableProducts method will be called.
     *
     * In production it will be run at 1 AM every morning
     * In all other deployments it will run every 15 minutes
     */
    @PostConstruct
    public void initializeScheduler() {
        ScheduleExpression expression = new ScheduleExpression();
        if (deployment.equals(Deployment.PROD)) {
            // 1 AM
            expression.hour("1");
        } else {
            // Every 5 minutes
            expression.minute("*/15").hour("*");
        }
        timerService.createCalendarTimer(expression,
                new TimerConfig("Timer to disable products in SAP which have been discontinued", false));
    }
}
