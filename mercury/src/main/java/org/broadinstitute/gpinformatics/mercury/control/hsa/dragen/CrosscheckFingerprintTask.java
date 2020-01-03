package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class CrosscheckFingerprintTask extends PicardTask {

    @Transient
    private File cramFile;

    @Transient
    private File haplotypeDatabse;

    @Transient
    private String outputPrefix;

    @Transient
    private File referenceSequence;

    /**
     * Syntax
     *  java -jar /seq/software/picard/current/bin/picard-private.jar CrosscheckFingerprints
     *  INPUT=/seq/dragen/aggregation/SM-J1738/19-11-01_02-56-44/SM-J1738-test.cram
     *  HAPLOTYPE_MAP=/seq/references/Homo_sapiens_assembly38/v0/Homo_sapiens_assembly38.haplotype_database.txt
     *  LOD_THRESHOLD=-20
     *  OUTPUT=sample.crosscheck_metrics_cram
     *  REFERENCE_SEQUENCE=/seq/references/Homo_sapiens_assembly38/v0/Homo_sapiens_assembly38.fasta
     */
    public CrosscheckFingerprintTask(File cramFile, File haplotypeDatabse, String outputPrefix,
                           int lodThreshold, File referenceSequence) {
        super("dragen");
        this.cramFile = cramFile;
        this.haplotypeDatabse = haplotypeDatabse;
        this.outputPrefix = outputPrefix;
        this.referenceSequence = referenceSequence;
        Objects.requireNonNull(cramFile, "cramFile must not be null.");
        Objects.requireNonNull(haplotypeDatabse, "haplotypeDatabse must not be null.");
        Objects.requireNonNull(outputPrefix, "outputPrefix must not be null.");
        Objects.requireNonNull(referenceSequence, "referenceSequence must not be null if cram.");

        PicardTaskBuilder taskBuilder = new PicardTaskBuilder()
                .tool(PicardTool.CROSSCHECK_FINGERPRINT.getToolName())
                .inputFile(cramFile)
                .haplotypeMap(haplotypeDatabse)
                .lodThreshold(lodThreshold)
                .outputPrefix(outputPrefix)
                .referenceSequence(referenceSequence);

        String picardTaskBuilder = taskBuilder.build();

        setCommandLineArgument(picardTaskBuilder);
    }

    public CrosscheckFingerprintTask() {
    }
}
