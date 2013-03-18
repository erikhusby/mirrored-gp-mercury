package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class ExtractTransformRunner {
    private Log logger = LogFactory.getLog(getClass());

    @Inject
    private ExtractTransform extractTransform;

    /**
     * JEE auto-schedules incremental ETL.
     */
    @Schedule(hour = "*", minute = "*/1", persistent = false)
    void scheduledEtl() {
        logger.info("XXX scheduledEtl");
        extractTransform.onDemandIncrementalEtl();
    }
}