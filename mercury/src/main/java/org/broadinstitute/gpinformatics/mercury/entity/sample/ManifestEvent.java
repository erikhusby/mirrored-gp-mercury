package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

/**
 * Manifest events represent logged items of interest that occur during the accessioning or tube scanning processes.
 * Tracking these items is a critical piece of supporting a quality system.
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(schema = "mercury", name = "MANIFEST_EVENT")
public class ManifestEvent implements Updatable {

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_EVENT", schema = "mercury", sequenceName = "SEQ_MANIFEST_EVENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_EVENT")
    @Column(name = "manifest_event_id")
    private Long manifestEventId;

    private String message;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = true)
    @JoinColumn(name = "manifest_record_id")
    private ManifestRecord manifestRecord;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession session;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Column(name = "MODIFIED_BY")
    private Long modifiedBy;

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    /**
     * For JPA
     */
    protected ManifestEvent() {
    }

    public ManifestEvent(Severity severity, String message) {
        this(severity, message, null);
    }

    public ManifestEvent(Severity severity, String message, ManifestRecord record) {
        this.severity = severity;
        this.message = message;
        if (record != null) {
            this.manifestRecord = record;
            this.manifestRecord.addManifestEvent(this);
        }
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public ManifestSession getSession() {
        return session;
    }

    public void setSession(ManifestSession session) {
        this.session = session;
    }

    @Override
    public void setModifiedDate(Date date) {
        this.modifiedDate = date;
    }

    @Override
    public Long getCreatedBy() {
        return this.createdBy;
    }

    @Override
    public Long getModifiedBy() {
        return this.modifiedBy;
    }

    @Override
    public void setModifiedBy(BspUser user) {
        this.modifiedBy = user.getUserId();
    }

    @Override
    public Date getModifiedDate() {
        return this.modifiedDate;
    }

    @Override
    public Date getCreatedDate() {
        return this.createdDate;
    }

    @Override
    public void setCreatedBy(BspUser createdBy) {
        this.createdBy = createdBy.getUserId();
    }

    @Override
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public void setModifiedBy(Long modifiedUserId) {
        this.modifiedBy = modifiedUserId;
    }

    public enum Severity {
        /**
         * A hard stop, e.g. duplicate sample ID within the same research project.
         */
        QUARANTINED,
        /**
         * Something like mismatched gender, where there is a problem but lab users are given the discretion to
         * continue processing a sample.
         */
        ERROR
    }

    @Override
    public String toString() {
        return "ManifestEvent{" +
               "message='" + message + '\'' +
               ", severity=" + severity +
               '}';
    }
}
