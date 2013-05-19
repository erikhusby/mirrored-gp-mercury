package org.broadinstitute.gpinformatics.mercury.entity.analysis;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A representation of the aligner that will be used for analysis.
 */
@Entity
@Audited
@Table(name = "ALIGNER", schema = "mercury")
public class Aligner {

    @Id
    @SequenceGenerator(name = "SEQ_ALIGNER", schema = "mercury", sequenceName = "SEQ_ALIGNER", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ALIGNER")
    private Long alignerId;

    @Column(name = "NAME")
    private String name;

    Aligner() {
    }

    public Aligner(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getBusinessKey() {
        return name;
    }
}
