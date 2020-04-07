package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class AlignmentTask extends AligntmentTaskBase {

    /**
     * Syntax
     * dragen -f -r /staging/reference/<reference> \
     * --fastq-list /path/to/fastq_list.csv \
     * --fastq-list-sample-id <Sample Name RGSM in fastq_list.csv> \
     * --output-directory /path/to/outputdir/<Sample Name> \
     * --intermediate-results-dir \
     * --output-file-prefix <Sample Name> \
     * --vc-sample-name <Sample Name> \
     * --enable-variant-caller true \
     * --enable-duplicate-marking true \
     * --enable-map-align-output false
     * --sample-sex FEMALE
     */
    public AlignmentTask(File reference, File fastQList, String fastQSampleId, File outputDirectory,
                         File intermediateResultsDir, String outputFilePrefix, String vcSampleName,
                         File qcContaminationFile, File qcCoverageBedFile, String sampleSex) {
        super("dragen");
        this.reference = reference;
        this.fastQList = fastQList;
        this.fastQSampleId = fastQSampleId;
        this.outputDir = outputDirectory;
        this.intermediateResultsDir = intermediateResultsDir;
        this.outputFilePrefix = outputFilePrefix;
        this.vcSampleName = vcSampleName;
        this.qcContaminationFile = qcContaminationFile;
        this.qcCoverageBedFile = qcCoverageBedFile;

        Objects.requireNonNull(reference, "reference directory must not be null.");
        Objects.requireNonNull(fastQList, "fastQList must not be null.");
        Objects.requireNonNull(fastQSampleId, "Sample Id must not be null.");
        Objects.requireNonNull(outputDirectory, "outputDir must not be null.");
        Objects.requireNonNull(intermediateResultsDir, "intermediateResultsDir must not be null.");
        Objects.requireNonNull(outputFilePrefix, "outputFilePrefix must not be null.");
        Objects.requireNonNull(vcSampleName, "vcSampleName must not be null.");
        Objects.requireNonNull(qcContaminationFile, "qcContaminationFile must not be null.");
        Objects.requireNonNull(qcCoverageBedFile, "qcCoverageBedFile must not be null.");

        DragenTaskBuilder taskBuilder = new DragenTaskBuilder().
                reference(reference).
                fastQList(fastQList).
                fastQSampleId(fastQSampleId).
                outputDirectory(outputDirectory).
                intermediateResultsDir(intermediateResultsDir).
                outputFilePrefix(outputFilePrefix).
                vcSampleName(vcSampleName).
                enableVariantCaller(true).
                enableDuplicateMarking(true).
                enableMapAlignOutput(true).
                qcCrossContaminationVcf(qcContaminationFile).
                qcCoverageRegion(qcCoverageBedFile).
                qcCoverageReports("cov_report");
//        if (sampleSex != null) {
//            taskBuilder = taskBuilder.sampleSex(sampleSex);
//        }

        String cmd = taskBuilder.build();

        setCommandLineArgument(cmd);
    }

    public AlignmentTask() {
    }

    public AlignmentState getAlignmentState() {
        return OrmUtil.proxySafeCast(getState(), AlignmentState.class);
    }
}
