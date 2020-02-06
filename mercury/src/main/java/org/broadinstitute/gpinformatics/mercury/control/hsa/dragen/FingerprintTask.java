package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class FingerprintTask extends ComputeTask {

    @Transient
    private File bamFile;

    @Transient
    private File genotypesFile;

    @Transient
    private File haplotypeDatabse;

    @Transient
    private String outputPrefix;

    @Transient
    private File referenceSequence;

    /**
     * Syntax
     *  java -jar picard.jar CheckFingerprint \
     *           INPUT=sample.bam \
     *           GENOTYPES=sample_genotypes.vcf \
     *           HAPLOTYPE_DATABASE=fingerprinting_haplotype_database.txt \
     *           OUTPUT=sample_fingerprinting
     * If its a CRAM file then reference sequence is required, ignored otherwise
     */
    public FingerprintTask(File bamFile, File genotypesFile, File haplotypeDatabse, String outputPrefix,
                           File referenceSequence) {
        this.bamFile = bamFile;
        this.genotypesFile = genotypesFile;
        this.haplotypeDatabse = haplotypeDatabse;
        this.outputPrefix = outputPrefix;
        this.referenceSequence = referenceSequence;
        Objects.requireNonNull(bamFile, "bamFile must not be null.");
        Objects.requireNonNull(genotypesFile, "genotypesFile must not be null.");
        Objects.requireNonNull(haplotypeDatabse, "haplotypeDatabse must not be null.");
        Objects.requireNonNull(outputPrefix, "outputPrefix must not be null.");

        boolean cram = this.bamFile.getName().endsWith("cram");
        if (cram) {
            Objects.requireNonNull(referenceSequence, "referenceSequence must not be null if cram.");
        }

        PicardTaskBuilder taskBuilder = new PicardTaskBuilder()
                .tool(PicardTool.CHECK_FINGERPRINT.getToolName())
                .inputFile(bamFile)
                .genotypeFile(genotypesFile)
                .haplotypeMap(haplotypeDatabse)
                .outputPrefix(outputPrefix);
        if (cram) {
            taskBuilder.referenceSequence(referenceSequence);
        }

        String picardTaskBuilder = taskBuilder.build();

        setCommandLineArgument(picardTaskBuilder);
    }

    public FingerprintTask() {
    }

    public File getBamFile() {
        if (bamFile == null) {
            bamFile = new File(PicardTaskBuilder.parseCommandFromArgument(
                    PicardTaskBuilder.INPUT, getCommandLineArgument()));
        }
        return bamFile;
    }

    public File getGenotypesFile() {
        if (genotypesFile == null) {
            bamFile = new File(PicardTaskBuilder.parseCommandFromArgument(
                    PicardTaskBuilder.INPUT, getCommandLineArgument()));
        }
        return genotypesFile;
    }

    public File getHaplotypeDatabse() {
        return haplotypeDatabse;
    }

    public String getOutputPrefix() {
        if (outputPrefix == null) {
            outputPrefix = PicardTaskBuilder.parseCommandFromArgument(
                    PicardTaskBuilder.OUTPUT, getCommandLineArgument());
        }
        return outputPrefix;
    }
}
