package org.broadinstitute.gpinformatics.mercury.entity.envers;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionNumber;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Contains the user-entered reason for a data fixup. A reason is needed in the audit trail.
 *
 * Each data fixup (currently done by a developer's fixup test) needs to persist a FixupCommentary
 * in the same transaction as the data change, in order for the reason to line up with the changed
 * entities in the audit trail.  The join from FixupCommentary to changed entities is made by the
 * AuditReader and is based on entity's implicit rev, not entity id.
 */
@Entity
@Audited
@Table(schema = "mercury")
public class FixupCommentary implements Serializable {
    @Id
    @SequenceGenerator(name = "SEQ_FIXUP_COMMENTARY", schema = "mercury", sequenceName = "SEQ_FIXUP_COMMENTARY")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FIXUP_COMMENTARY")
    @RevisionNumber
    private Long fixupCommentaryId;

    private String reason;

    public FixupCommentary() {
    }

    public Long getFixupCommentaryId(){
        return fixupCommentaryId;
    }

    public FixupCommentary(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}