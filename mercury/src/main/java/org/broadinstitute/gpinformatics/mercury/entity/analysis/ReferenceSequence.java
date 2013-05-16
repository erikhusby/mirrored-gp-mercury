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
@Table(name = "REFERENCE_SEQUENCE", schema = "mercury")
public class ReferenceSequence {

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}
