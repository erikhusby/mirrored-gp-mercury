package org.broadinstitute.gpinformatics.infrastructure.datawh;

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

    @Inject
    private ExtractTransform extractTransform;

    /**
     * JEE auto-schedules incremental ETL.
     */
    @Schedule(hour = "*", minute = "*/15", persistent = false)
    void scheduledEtl() {
        extractTransform.onDemandIncrementalEtl();
    }
}