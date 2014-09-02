package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
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
 * Manifest events represents logged items of interest that occur during the registration and/or accessioning
 * process.  Tracking these items is a critical piece of supporting a quality system.
 */
@Entity
@Audited
@Table(schema = "mercury", name="MANIFEST_EVENT")
public class ManifestEvent {

    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_EVENT", schema = "mercury", sequenceName = "SEQ_MANIFEST_EVENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_EVENT")
    @Column(name="manifest_event_id")
    private Long manifestEventId;

    private String message;

    @ManyToOne(cascade = CascadeType.PERSIST,optional = true)
    @JoinColumn(name ="manifest_record_id")
    private ManifestRecord manifestRecord;

    @Enumerated(EnumType.STRING)
    private Type logType;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession session;

    /** For JPA */
    protected ManifestEvent() {
    }

    public ManifestEvent(String message, Type logType) {
        this(message, logType, null);
    }

    public ManifestEvent(String message, Type logType, ManifestRecord record) {
        this.message = message;
        this.logType = logType;
        if (record != null) {
            this.manifestRecord = record;
            this.manifestRecord.addLogEntry(this);
        }
    }

    public Type getLogType() {
        return logType;
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

    public ManifestRecord getManifestRecord() {
        return manifestRecord;
    }

    public enum Type {
        /**
         * A hard stop, something like a missing or duplicate sample.
         */
        QUARANTINED,
        /**
         * Something like mismatched gender, where there is a problem but lab users are given the discretion to
         * continue processing a sample.
         */
        ERROR,
        /**
         * Informational message for an error that occurs but may be fixable
         */
        WARNING

    }
}
