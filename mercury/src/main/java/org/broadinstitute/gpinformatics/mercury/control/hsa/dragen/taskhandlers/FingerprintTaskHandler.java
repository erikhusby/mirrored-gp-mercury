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
    private SnpListDao snpListDao;

    @Inject
    private FingerprintResource fingerprintResource;

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

        // TODO get name somehow or just always use this for now
        // TODO not in a transaction
        // TODO Remove
        SnpList snpList = snpListDao.findByName("HG19HaplotypeDbSnps");

        FingerprintBean fingerprintBean = handleTaskDaoFree(fpTask, fingerprintState.getMercurySample(), snpList);
        if (fingerprintBean != null) {
            String postResult = fingerprintResource.post(fingerprintBean);
            if (postResult == null || !postResult.equals("Stored fingerprint")) {
                fpTask.setErrorMessage("Failed to create fingerprint for " + fingerprintState.getMercurySample().getSampleKey());
                fpTask.setStatus(Status.FAILED);
            } else {
                // TODO Handle success and suspended
                fpTask.setStatus(Status.COMPLETE);
            }
        }
    }

    @DaoFree
    public FingerprintBean handleTaskDaoFree(FingerprintTask task, MercurySample mercurySample, SnpList snpList) {
        File bamFile = task.getBamFile();
        File parentFile = bamFile.getParentFile();
        String outputPrefix = task.getOutputPrefix();

        File summaryFile = new File(parentFile, String.format(FINGERPRINTING_SUMMARY_METRICS, outputPrefix));
        File detailFile = new File(parentFile, String.format(FINGERPRINTING_DETAIL_METRICS, outputPrefix));

        if (!summaryFile.exists() || !detailFile.exists()) {
            task.setErrorMessage("Alignment summary and detail files don't exist in " + parentFile.getPath());
            task.setStatus(Status.FAILED);
            return null;
        }

        // TODO Some magic to get reference sequence from Mercury Sample (SampleInstanceV2 has it)
        String refSeqName = "Homo_sapiens_assembly38";

        MetricsFile<FingerprintingDetailMetrics, ?> fingerprintingDetailMetrics = loadMetricsIfPresent(detailFile);

        //TODO How do I know which reference it came from? See above, need some map?
        Map<String, Snp> mapRsIdToSnp = snpList.getMapRsIdToSnp();

        Fingerprint.Gender gender = Fingerprint.Gender.byDisplayname(mercurySample.getSampleData().getGender());
        if (gender == null) {
            gender = Fingerprint.Gender.UNKNOWN;
        }

        FingerprintBean fingerprintBean = new FingerprintBean();
        fingerprintBean.setPlatform(Fingerprint.Platform.GENERAL_SEQUENCING.name());
        fingerprintBean.setDisposition(Fingerprint.Disposition.PASS.getAbbreviation()); // TODO based on some threshold (qscore?)
        fingerprintBean.setGender(gender.getAbbreviation());// TODO just assume gender from sample data?
        fingerprintBean.setGenomeBuild(Fingerprint.GenomeBuild.HG38.name());// TODO build later
        fingerprintBean.setSnpListName(snpList.getName());
        fingerprintBean.setDateGenerated(new Date());
        fingerprintBean.setAliquotLsid(mercurySample.getSampleData().getSampleLsid()); // TODO
        fingerprintBean.setQueriedLsid(mercurySample.getSampleData().getSampleLsid());

        List<FingerprintCallsBean> fingerprintCallBeans = new ArrayList<>();
        fingerprintBean.setCalls(fingerprintCallBeans);
        MessageCollection messageCollection = new MessageCollection();
        for (FingerprintingDetailMetrics metrics: fingerprintingDetailMetrics.getMetrics()) {
            Snp snp = mapRsIdToSnp.get(metrics.SNP);
            if (snp == null) {
                messageCollection.addError("Failed to find snp " + metrics.SNP + " in snplist " + snpList.getName());
            } else {
                FingerprintCallsBean fingerprintCallsBean = new FingerprintCallsBean(metrics.SNP,
                        metrics.OBSERVED_GENOTYPE,  null); //TODO Call confidence? gq/pl? lod score?
                fingerprintCallBeans.add(fingerprintCallsBean);
            }
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
