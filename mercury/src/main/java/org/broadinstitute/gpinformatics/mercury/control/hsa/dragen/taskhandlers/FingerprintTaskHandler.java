package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import htsjdk.samtools.metrics.MetricBase;
import htsjdk.samtools.metrics.MetricsFile;
import htsjdk.samtools.util.CloserUtil;
import htsjdk.samtools.util.IOUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintCallsBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintUploadTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import picard.analysis.FingerprintingDetailMetrics;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Dependent
public class FingerprintTaskHandler extends AbstractTaskHandler {

    private static final Log log = LogFactory.getLog(FingerprintTaskHandler.class);

    private static final String FINGERPRINTING_SUMMARY_METRICS = "%s.fingerprinting_summary_metrics";
    private static final String FINGERPRINTING_DETAIL_METRICS = "%s.fingerprinting_detail_metrics";

    @Inject
    private FingerprintResource fingerprintResource;

    @Inject
    private FingerprintEjb fingerprintEjb;

    @Override
    public void handleTask(Task task, SchedulerContext schedulerContext) {
        FingerprintUploadTask fpUploadTask = OrmUtil.proxySafeCast(task, FingerprintUploadTask.class);
        State state = fpUploadTask.getState();
        if (!OrmUtil.proxySafeIsInstance(state, FingerprintState.class)) {
            task.setErrorMessage("Expect only a fingerprint state for a fingerprint metrics task.");
            task.setStatus(Status.FAILED);
            return;
        }

        FingerprintState fingerprintState = OrmUtil.proxySafeCast(state, FingerprintState.class);
        Task picardTask = fingerprintState.getTasks().iterator().next();
        if (!OrmUtil.proxySafeIsInstance(picardTask, FingerprintTask.class)) {
            task.setErrorMessage("Expect only a fingerprint task in a fingerprint state.");
            task.setStatus(Status.FAILED);
            return;
        }

        FingerprintTask fpTask = OrmUtil.proxySafeCast(picardTask, FingerprintTask.class);

        FingerprintBean fingerprintBean = handleTaskDaoFree(fpTask, fingerprintState.getMercurySample());
        if (fingerprintBean != null) {
            // TODO I don't know what this will do
            Fingerprint fingerprint = null;// fingerprintEjb.handleNewFingerprint(fingerprintBean, fingerprintState.getMercurySample());

            if (fingerprint == null) {
                fpTask.setErrorMessage("Failed to create fingerprint for " + fingerprintState.getMercurySample().getSampleKey());
                fpTask.setStatus(Status.FAILED);
            } else {
                // TODO Handle success and suspended
                fpTask.setStatus(Status.COMPLETE);
            }
        } else {
            fpTask.setStatus(Status.FAILED);
            fpTask.setErrorMessage("Failed to build fingerprint bean");
        }
    }

    @DaoFree
    public FingerprintBean handleTaskDaoFree(FingerprintTask task, MercurySample mercurySample) {
        String outputPrefix = task.getOutputPrefix();

        File summaryFile = new File(String.format(FINGERPRINTING_SUMMARY_METRICS, outputPrefix));
        File detailFile = new File(String.format(FINGERPRINTING_DETAIL_METRICS, outputPrefix));

        if (!summaryFile.exists() || !detailFile.exists()) {
            task.setErrorMessage("Alignment summary and detail files don't exist in " + detailFile.getPath());
            task.setStatus(Status.FAILED);
            return null;
        }

        // TODO Some magic to get reference sequence from Mercury Sample (SampleInstanceV2 has it)
        String refSeqName = "Homo_sapiens_assembly38";

        MetricsFile<FingerprintingDetailMetrics, ?> fingerprintingDetailMetrics = loadMetricsIfPresent(detailFile);

        // TODO This uses a dao
        Fingerprint.Gender gender = Fingerprint.Gender.byDisplayname(mercurySample.getSampleData().getGender());
        if (gender == null) {
            gender = Fingerprint.Gender.UNKNOWN;
        }

        FingerprintBean fingerprintBean = new FingerprintBean();
        fingerprintBean.setPlatform(Fingerprint.Platform.GENERAL_SEQUENCING.name());
        fingerprintBean.setDisposition(Fingerprint.Disposition.PASS.getAbbreviation()); // TODO based on some threshold (qscore?)
        fingerprintBean.setGender(gender.getAbbreviation());// TODO just assume gender from sample data?
        fingerprintBean.setGenomeBuild(Fingerprint.GenomeBuild.HG38.name());// TODO build later
        fingerprintBean.setSnpListName("HG19HaplotypeDbSnps");
        fingerprintBean.setDateGenerated(new Date());
        fingerprintBean.setAliquotLsid(mercurySample.getSampleData().getSampleLsid()); // TODO
        fingerprintBean.setQueriedLsid(mercurySample.getSampleData().getSampleLsid());

        List<FingerprintCallsBean> fingerprintCallBeans = new ArrayList<>();
        fingerprintBean.setCalls(fingerprintCallBeans);
        for (FingerprintingDetailMetrics metrics: fingerprintingDetailMetrics.getMetrics()) {
            FingerprintCallsBean fingerprintCallsBean = new FingerprintCallsBean(metrics.SNP,
                    metrics.OBSERVED_GENOTYPE,  null); //TODO Call confidence? gq/pl? lod score?
            fingerprintCallBeans.add(fingerprintCallsBean);
        }

        return fingerprintBean;
    }

    private <M extends MetricBase> MetricsFile<M,?> loadMetricsIfPresent(final File file) {
        IOUtil.assertFileIsReadable(file);

        final MetricsFile<M,?> metrics = new MetricsFile<>();
        final InputStreamReader in = new InputStreamReader(IOUtil.openFileForReading(file));
        metrics.read(in);
        CloserUtil.close(in);

        return metrics;
    }
}
