package org.broadinstitute.gpinformatics.mercury.entity.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This gives the pipeline the reference sequence that should be used when generating data. This will be one item
 * from a set list that is determined (and managed) by the pipeline team.
 */
@Entity
@Audited
@Table(name = "REFERENCE_SEQUENCE", schema = "mercury")
public class ReferenceSequence implements BusinessObject {
    @Id
    @SequenceGenerator(name = "SEQ_REFERENCE_SEQUENCE", schema = "mercury", sequenceName = "SEQ_REFERENCE_SEQUENCE", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REFERENCE_SEQUENCE")
    private Long sequenceId;

    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "VERSION", length = 50, nullable = false)
    private String version;

    @Column(name = "IS_CURRENT", nullable = false)
    private boolean isCurrent;

    public static final char SEPARATOR = '|';

    // NO_REFERENCE_SEQUENCE puts a null value in the pipeline query.
    public static final String NO_REFERENCE_SEQUENCE = "No_Reference_Sequence";

    public ReferenceSequence() {
    }

    public ReferenceSequence(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }

    @Override
    public String getBusinessKey() {
        return name + SEPARATOR + version;
    }
}
