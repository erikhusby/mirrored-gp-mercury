package org.broadinstitute.gpinformatics.mercury.entity.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Nameable;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A representation of the sequence aligner that will be used for analysis. This is managed by the pipeline team in a
 * user interface provided by Mercury.
 */
@Entity
@Audited
@Table(name = "ALIGNER", schema = "mercury")
public class Aligner implements BusinessKeyable {
    @Id
    @SequenceGenerator(name = "SEQ_ALIGNER", schema = "mercury", sequenceName = "SEQ_ALIGNER", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ALIGNER")
    private Long alignerId;

    @Column(name = "NAME")
    private String name;

    public Aligner(Long alignerId, String name) {
        this.alignerId = alignerId;
        this.name = name;
    }

    protected Aligner() {
    }

    public Aligner(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getBusinessKey() {
        return name;
    }
}
