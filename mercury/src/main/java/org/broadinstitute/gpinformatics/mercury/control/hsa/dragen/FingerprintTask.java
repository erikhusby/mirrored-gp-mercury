package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.io.File;
import java.util.Objects;

@Entity
@Audited
public class FingerprintTask extends ProcessTask {

    /**
     * Syntax
     *  java -jar picard.jar CheckFingerprint \
     *           INPUT=sample.bam \
     *           GENOTYPES=sample_genotypes.vcf \
     *           HAPLOTYPE_DATABASE=fingerprinting_haplotype_database.txt \
     *           OUTPUT=sample_fingerprinting
     */
    public FingerprintTask(File bamFile, File genotypesFile, File haplotypeDatabse, String outputPrefix) {
        super("dragen");
        Objects.requireNonNull(bamFile, "bamFile must not be null.");
        Objects.requireNonNull(genotypesFile, "genotypesFile must not be null.");
        Objects.requireNonNull(haplotypeDatabse, "haplotypeDatabse must not be null.");
        Objects.requireNonNull(outputPrefix, "outputPrefix must not be null.");

        String picardTaskBuilder = new PicardTaskBuilder()
                .tool(PicardTool.CHECK_FINGERPRINT.getToolName())
                .inputFile(bamFile)
                .genotypeFile(genotypesFile)
                .haplotypeMap(haplotypeDatabse)
                .outputPrefix(outputPrefix)
                .build();

        setCommandLineArgument(picardTaskBuilder);
    }

    public FingerprintTask() {
    }
}
