package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.HasUpdateData;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.CascadeType;
import javax.persistence.Embedded;
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

/**
 * Manifest events represent logged items of interest that occur during the accessioning or tube scanning processes.
 * Tracking these items is a critical piece of supporting a quality system.
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(schema = "mercury", name = "MANIFEST_EVENT")
public class ManifestEvent implements HasUpdateData {

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_EVENT", schema = "mercury", sequenceName = "SEQ_MANIFEST_EVENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_EVENT")
    @Column(name = "manifest_event_id")
    private Long manifestEventId;

    private String message;

    // IntelliJ wants to convert this to a local, not one of its better suggestions.
    @SuppressWarnings("FieldCanBeLocal")
    @ManyToOne(cascade = CascadeType.PERSIST, optional = true)
    @JoinColumn(name = "manifest_record_id")
    private ManifestRecord manifestRecord;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession session;

    // IntelliJ claims this is unused.
    @SuppressWarnings("UnusedDeclaration")
    @Embedded
    private UpdateData updateData = new UpdateData();

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

    public UpdateData getUpdateData() {
        return updateData;
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
