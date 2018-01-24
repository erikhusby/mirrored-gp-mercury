package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents a Single Nucleotide Polymorphism
 */
@Entity
@Audited
@Table(schema = "mercury")
public class Snp {

    @SuppressWarnings("unused")
    @SequenceGenerator(name = "SEQ_SNP", schema = "mercury", sequenceName = "SEQ_SNP")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SNP")
    @Id
    private Long snpId;

    private String rsId;

    private Boolean isFailed;

    private Boolean isGender;

    public Snp(String rsId) {
        this.rsId = rsId;
    }

    public Snp(String rsId, Boolean isFailed, Boolean isGender) {
        this.rsId = rsId;
        this.isFailed = isFailed;
        this.isGender = isGender;
    }

    /** For JPA. */
    protected Snp() {
    }

    public String getRsId() {
        return rsId;
    }

    public Boolean isGender() {
        return isGender == null ? false : isGender;
    }

    public Boolean isFailed() {
        return isFailed == null ? false : isFailed;
    }
}
