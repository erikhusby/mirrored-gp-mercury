package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Represents a Single Nucleotide Polymorphism in a Fingerprint.
 */
@Entity
@Audited
@Table(schema = "mercury",
    indexes = {@Index(name = "IX_FPG_FINGERPRINT", columnList = "FINGERPRINT", unique = false)})
public class FpGenotype {

    @SuppressWarnings("unused")
    @SequenceGenerator(name = "SEQ_FP_GENOTYPE", schema = "mercury", sequenceName = "SEQ_FP_GENOTYPE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FP_GENOTYPE")
    @Id
    private Long fpGenotypeId;

    @ManyToOne
    @JoinColumn(name = "FINGERPRINT")
    private Fingerprint fingerprint;

    @ManyToOne
    @JoinColumn(name = "SNP")
    private Snp snp;

    private String genotype;

    private BigDecimal callConfidence;

    public FpGenotype(Fingerprint fingerprint, Snp snp, String genotype, BigDecimal callConfidence) {
        this.fingerprint = fingerprint;
        this.snp = snp;
        this.genotype = genotype;
        this.callConfidence = callConfidence;
    }

    /** For JPA. */
    protected FpGenotype() {
    }

    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public Snp getSnp() {
        return snp;
    }

    public String getGenotype() {
        return genotype;
    }

    public BigDecimal getCallConfidence() {
        return callConfidence;
    }
}
