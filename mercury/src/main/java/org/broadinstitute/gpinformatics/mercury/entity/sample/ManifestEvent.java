package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
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
import java.util.List;
import java.util.Set;

/**
 * Manifest events represent logged items of interest that occur during the accessioning or tube scanning processes.
 * Tracking these items is a critical piece of supporting a quality system.
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(schema = "mercury", name = "MANIFEST_EVENT")
public class ManifestEvent implements Updatable {

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
    private ManifestSession manifestSession;

    @Embedded
    private UpdateData updateData = new UpdateData();

    /**
     * For JPA.
     */
    protected ManifestEvent() {
    }

    public ManifestEvent(ManifestRecord.ErrorStatus severityError, String message, ManifestRecord record) {
        this(severityError.getSeverity(), message, record);
    }

    public ManifestEvent(Severity severity, String message, ManifestRecord record) {
        this.severity = severity;
        this.message = message;
        if (record != null) {
            manifestRecord = record;
            manifestRecord.addManifestEvent(this);
        }
    }

    /**
     * Helper method to check if there are any manifest event entries of a given type
     *
     * @param manifestEvents    List of manifest events within which to find one that meets the given severity
     * @param severities        set of types to check for entries against
     *
     * @return true if even one event entry matches a type in the given set of event types
     */
    static boolean hasManifestEventOfType(List<ManifestEvent> manifestEvents,
                                          Set<Severity> severities) {
        for (ManifestEvent manifestEvent : manifestEvents) {
            if (severities.contains(manifestEvent.getSeverity())) {
                return true;
            }
        }
        return false;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public void setManifestSession(ManifestSession session) {
        this.manifestSession = session;
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
        ERROR,
        WARNING
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("manifestEventId", manifestEventId)
                .append("message", message)
                .append("severity", severity)
                .append("sessionName", manifestSession.getSessionName())
                .toString();
    }

    @Override
    public UpdateData getUpdateData() {
        return updateData;
    }
}
