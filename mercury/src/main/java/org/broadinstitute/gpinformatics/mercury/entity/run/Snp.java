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
 * todo jmt call this GeneticVariation?
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

    public Snp(String rsId) {
        this.rsId = rsId;
    }

    /** For JPA. */
    protected Snp() {
    }

    public String getRsId() {
        return rsId;
    }
}
