package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.analytics.FingerprintScoreDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.FingerprintScore;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AggregationTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AggregationState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.control.run.FingerprintEjb;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.presentation.hsa.AggregationActionBean;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class FingerprintTaskHandler extends AbstractTaskHandler<FingerprintTask> {

    private static final Log log = LogFactory.getLog(FingerprintTaskHandler.class);

    @Inject
    private FingerprintEjb fingerprintEjb;

    @Inject
    private FingerprintScoreDao fingerprintScoreDao;

    @Inject
    private ConcordanceCalculator concordanceCalculator;

    @Override
    public void handleTask(FingerprintTask task, SchedulerContext schedulerContext) {
        State state = task.getState();
        if (!OrmUtil.proxySafeIsInstance(state, FingerprintState.class)) {
            task.setErrorMessage("Expect only a fingerprint state for a fingerprint metrics task.");
            task.setStatus(Status.FAILED);
            return;
        }

        FingerprintState fingerprintState = OrmUtil.proxySafeCast(state, FingerprintState.class);

        File vcfFile = task.getGenotypesFile();
        if (vcfFile == null) {
            task.setStatus(Status.FAILED);
            task.setErrorMessage("No VCF in task arguments.");
            return;
        } else if (!vcfFile.exists()) {
            // Check for hard filtered - (Would also be in arguments of config file)
            String path = vcfFile.getPath();
            path = path.replaceAll("vcf.gz", "hard-filtered.vcf.gz");
            vcfFile = new File(path);
            if (vcfFile.exists()) {
                task.setStatus(Status.FAILED);
                task.setErrorMessage("VCF doesn't exist: " + vcfFile.getPath());
            }
        }

        Optional<AggregationState> aggregationStateOpt =
                fingerprintState.getMercurySample().getMostRecentStateOfType(AggregationState.class);
        if (!aggregationStateOpt.isPresent()) {
            task.setStatus(Status.FAILED);
            task.setErrorMessage("No aggregation state found.");
            return;
        }
        AggregationState aggregationState = aggregationStateOpt.get();
        AggregationTask aggregationTask = aggregationState.getAggregationTask().get();

        File referenceFile = aggregationTask.getReference();
        AggregationActionBean.ReferenceGenome referenceGenome =
                AggregationActionBean.ReferenceGenome.getByDragenPath(referenceFile.getPath());

        if (referenceGenome == null) {
            task.setStatus(Status.FAILED);
            task.setErrorMessage("Failed to find reference genome: " + referenceFile.getPath());
            return;
        }

        MercurySample mercurySample = fingerprintState.getMercurySample();
        Map<String, MercurySample> mapKeyToSample = new HashMap<>();
        String sampleKey = mercurySample.getSampleKey();
        mapKeyToSample.put(sampleKey, mercurySample);
        List<Fingerprint> fingerprints = fingerprintEjb.findFingerprints(mapKeyToSample);

        List<Fingerprint> fluidigmFingerprints = fingerprints.stream()
                .filter(fp -> fp.getPlatform() == Fingerprint.Platform.FLUIDIGM &&
                              fp.getDisposition() == Fingerprint.Disposition.PASS)
                .sorted(Comparator.comparing(Fingerprint::getDateGenerated))
                .collect(Collectors.toList());

        Comparator<Fingerprint> comparator = (o1, o2) -> new CompareToBuilder().
                append(o1.getPlatform().getPrecedenceForInitial(), o2.getPlatform().getPrecedenceForInitial()).
                append(o2.getDateGenerated(), o1.getDateGenerated()).
                build();

        Optional<Fingerprint> optionalFingerprint = fluidigmFingerprints.stream().max(comparator);
        if (!optionalFingerprint.isPresent()) {
            task.setStatus(Status.RUNNING);
            task.setErrorMessage("No Fluidigm Fingerprints present.");
            return;
        }

        Fingerprint fluidigmFingerprint = optionalFingerprint.get();

        String haplotypeDatabase = referenceGenome.getHaplotypeDatabase();
        String fasta = referenceGenome.getFasta();

        try {
            double lodScore = concordanceCalculator
                    .calculateAggregationLodScore(sampleKey, fluidigmFingerprint, vcfFile.getPath(),
                            haplotypeDatabase, fasta);
            FingerprintScore fpScore = new FingerprintScore();
            fpScore.setLodScore(BigDecimal.valueOf(lodScore));
            fpScore.setSampleAlias(sampleKey);
            fpScore.setRunName(sampleKey + "_Aggregation");
            fpScore.setRunDate(new Date());
            fpScore.setAnalysisName(aggregationTask.getOutputDir().getParentFile().getPath());
            fingerprintScoreDao.persist(fpScore);
            task.setStatus(Status.COMPLETE);
        } catch (Exception e) {
            task.setErrorMessage("Failed to calculate lod score for " + sampleKey);
            task.setStatus(Status.FAILED);
        }
    }
}
