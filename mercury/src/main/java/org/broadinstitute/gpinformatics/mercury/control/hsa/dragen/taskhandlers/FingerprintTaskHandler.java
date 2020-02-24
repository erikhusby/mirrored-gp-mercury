package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.taskhandlers;

//import htsjdk.samtools.metrics.MetricBase;
//import htsjdk.samtools.metrics.MetricsFile;
//import htsjdk.samtools.util.CloserUtil;
//import htsjdk.samtools.util.IOUtil;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.analytics.FingerprintScoreDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.FingerprintScore;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintCallsBean;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.SampleSheetBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.FingerprintUploadTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.scheduler.SchedulerContext;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FingerprintState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.run.FingerprintEjb;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
//import picard.analysis.FingerprintingDetailMetrics;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class FingerprintTaskHandler extends AbstractTaskHandler<FingerprintUploadTask> {

    private static final Log log = LogFactory.getLog(FingerprintTaskHandler.class);

    private static final String FINGERPRINTING_SUMMARY_METRICS = "%s.fingerprinting_summary_metrics";
    private static final String FINGERPRINTING_DETAIL_METRICS = "%s.fingerprinting_detail_metrics";

    @Inject
    private FingerprintEjb fingerprintEjb;

    @Inject
    private FingerprintScoreDao fingerprintScoreDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots samplesFromAliquots;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Override
    public void handleTask(FingerprintUploadTask task, SchedulerContext schedulerContext) {
        State state = task.getState();
        if (!OrmUtil.proxySafeIsInstance(state, FingerprintState.class)) {
            task.setErrorMessage("Expect only a fingerprint state for a fingerprint metrics task.");
            task.setStatus(Status.FAILED);
            return;
        }
        // TODO Handle multiple fingerprint tasks in a state
        FingerprintState fingerprintState = OrmUtil.proxySafeCast(state, FingerprintState.class);
        Task picardTask = fingerprintState.getTasks().iterator().next();
        if (!OrmUtil.proxySafeIsInstance(picardTask, FingerprintTask.class)) {
            task.setErrorMessage("Expect only a fingerprint task in a fingerprint state.");
            task.setStatus(Status.FAILED);
            return;
        }

        FingerprintTask fpTask = OrmUtil.proxySafeCast(picardTask, FingerprintTask.class);
        File laneFolder = fpTask.getBamFile().getParentFile();
        File fastq = laneFolder.getParentFile();
        File analysisFile = fastq.getParentFile();
        String analysisName = analysisFile.getName();

        MercurySample mercurySample = fingerprintState.getMercurySample();
        IlluminaSequencingRunChamber runChamber = fingerprintState.getSequencingRunChambers().iterator().next();
        RunCartridge flowcell = runChamber.getIlluminaSequencingRun().getSampleCartridge();

        FingerprintBean fingerprintBean = handleTaskDaoFree(fpTask, mercurySample);

        Pair<MercurySample, SampleInstanceV2> instancePair = SampleSheetBuilder.
                findSampleInFlowcellLane(flowcell, runChamber.getLanePosition(), mercurySample);
        MercurySample exportedSample = instancePair.getLeft();

        List<Fingerprint> fluidigmFingerprints = exportedSample.getFingerprints().stream()
                .filter(fp -> fp.getPlatform() == Fingerprint.Platform.FLUIDIGM &&
                              fp.getDisposition() == Fingerprint.Disposition.PASS)
                .sorted(Comparator.comparing(Fingerprint::getDateGenerated))
                .collect(Collectors.toList());

        Comparator<Fingerprint> comparator = (o1, o2) -> new CompareToBuilder().
                append(o1.getPlatform().getPrecedenceForInitial(), o2.getPlatform().getPrecedenceForInitial()).
                append(o2.getDateGenerated(), o1.getDateGenerated()).
                build();

        // Check BSP Export
        if (fluidigmFingerprints.isEmpty()) {

            List<BSPGetExportedSamplesFromAliquots.ExportedSample> exportedSamples =
                    samplesFromAliquots.getExportedSamplesFromAliquots(Collections.singleton(exportedSample.getSampleData().getSampleLsid()),
                            IsExported.ExternalSystem.GAP);
            List<String> sampleKeys = new ArrayList<>();
            for (BSPGetExportedSamplesFromAliquots.ExportedSample gapExportedSample : exportedSamples) {
                sampleKeys.add(FingerprintResource.getSmIdFromLsid(gapExportedSample.getExportedLsid()));
            }

            EnumSet<Fingerprint.Platform> initialPlatforms = EnumSet.of(
                    Fingerprint.Platform.FLUIDIGM, Fingerprint.Platform.GENERAL_ARRAY, Fingerprint.Platform.FAT_PANDA);

            Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleKeys);
            for (Map.Entry<String, MercurySample> idMercurySampleEntry : mapIdToMercurySample.entrySet()) {
                MercurySample sample = idMercurySampleEntry.getValue();
                if (sample != null) {
                    sample.getFingerprints().stream().
                            filter(fingerprint -> fingerprint.getDisposition() == Fingerprint.Disposition.PASS &&
                                                  initialPlatforms.contains(fingerprint.getPlatform())).
                            max(comparator).
                            ifPresent(fluidigmFingerprints::add);
                }
            }
        }

        Optional<Fingerprint> optionalFingerprint = fluidigmFingerprints.stream().max(comparator);
        if (!optionalFingerprint.isPresent()) {
            task.setStatus(Status.SUSPENDED);
            task.setErrorMessage("No Fluidigm Fingerprints.");

            return;
        }

        Fingerprint fluidigmFingerprint = optionalFingerprint.get();

        if (fingerprintBean != null) {
            Double lodScore = fingerprintEjb.handleNewFingerprint(fingerprintBean, mercurySample, fluidigmFingerprint);

            if (lodScore == null) {
                task.setErrorMessage("Failed to create lod score for " + mercurySample.getSampleKey());
                task.setStatus(Status.FAILED);
            } else {
                FingerprintScore fpScore = new FingerprintScore();
                fpScore.setLodScore(BigDecimal.valueOf(lodScore));
                fpScore.setSampleAlias(mercurySample.getSampleKey());
                fpScore.setRunName(fingerprintState.getRun().getRunName());
                fpScore.setLane(runChamber.getLaneNumber());
                fpScore.setFlowcell(flowcell.getLabel());
                fpScore.setRunDate(new Date());
                fpScore.setAnalysisName(analysisName);
                fingerprintScoreDao.persist(fpScore);
                task.setStatus(Status.COMPLETE);
            }
        } else {
            task.setStatus(Status.FAILED);
            task.setErrorMessage("Failed to build fingerprint bean");
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

//        MetricsFile<FingerprintingDetailMetrics, ?> fingerprintingDetailMetrics = loadMetricsIfPresent(detailFile);

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
//        for (FingerprintingDetailMetrics metrics: fingerprintingDetailMetrics.getMetrics()) {
//            FingerprintCallsBean fingerprintCallsBean = new FingerprintCallsBean(metrics.SNP,
//                    metrics.OBSERVED_GENOTYPE,  null); //TODO Call confidence? gq/pl? lod score?
//            fingerprintCallBeans.add(fingerprintCallsBean);
//        }

        return fingerprintBean;
    }

    /*
    private <M extends MetricBase> MetricsFile<M,?> loadMetricsIfPresent(final File file) {
        IOUtil.assertFileIsReadable(file);

        final MetricsFile<M,?> metrics = new MetricsFile<>();
        final InputStreamReader in = new InputStreamReader(IOUtil.openFileForReading(file));
        metrics.read(in);
        CloserUtil.close(in);

        return metrics;
    }*/
}
