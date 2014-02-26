package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Entity
@Audited
@Table(schema = "mercury", name = "rework_reason")
public class ReworkReason {

    @SequenceGenerator(name = "SEQ_REWORK_REASON", schema = "mercury",  sequenceName = "SEQ_REWORK_REASON")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REWORK_REASON")
    @Id
    @Column(name = "rework_reason_id")
    private Long reworkReasonId;

    @Column(name = "reason", nullable = false)
    private String reason;

    public ReworkReason(String reason) {
        this.reason = reason;
    }

    protected ReworkReason() {
    }

    public String getReason() {
        return reason;
    }

    public Long getReworkReasonId() {
        return reworkReasonId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReworkReason)) {
            return false;
        }

        ReworkReason that = (ReworkReason) o;

        if (!reason.equals(that.reason)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(reason).build();
    }
}
