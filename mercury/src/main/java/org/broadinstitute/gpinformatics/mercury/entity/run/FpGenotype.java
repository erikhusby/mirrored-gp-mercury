package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents a Single Nucleotide Polymorphism in a Fingerprint.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class FpGenotype {
    @SuppressWarnings("unused")
    @SequenceGenerator(name = "SEQ_FP_GENOTYPE", schema = "mercury", sequenceName = "SEQ_FP_GENOTYPE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FP_GENOTYPE")
    @Id
    private Long fpGenotypeId;

    @ManyToOne
    private Fingerprint fingerprint;

}
