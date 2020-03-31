package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.broadinstitute.gpinformatics.infrastructure.analytics.FingerprintScoreDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.FingerprintScore;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import java.math.BigDecimal;
import java.util.Date;

public class FingerprintTaskHandlerSimulator extends FingerprintTaskHandler {

    @Override
    public void handleTask(FingerprintTask task, SchedulerContext schedulerContext) {
        FingerprintState fingerprintState = OrmUtil.proxySafeCast(task.getState(), FingerprintState.class);
        MercurySample mercurySample = fingerprintState.getMercurySample();
        String sampleKey = mercurySample.getSampleKey();
        FingerprintScore fpScore = new FingerprintScore();
        fpScore.setLodScore(BigDecimal.valueOf(12));
        fpScore.setSampleAlias(sampleKey);
        fpScore.setRunName(sampleKey + "_Aggregation");
        fpScore.setRunDate(new Date());
        fpScore.setAnalysisName("simulated");

        FingerprintScoreDao dao = ServiceAccessUtility.getBean(FingerprintScoreDao.class);
        dao.persist(fpScore);
        task.setStatus(Status.COMPLETE);
    }
}
