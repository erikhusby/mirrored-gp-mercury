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
 * TODO scottmat fill in javadoc!!!
 */
@Entity
@Audited
@Table(schema = "mercury", name="MANIFEST_EVENT")
public class ManifestEvent {

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

        this.message = message;
        this.logType = logType;
    }

    public ManifestEvent(String message, ManifestRecord record, Type logType) {

        this.message = message;
        setManifestRecord(record);
        this.logType = logType;
    }

    public void setManifestRecord(ManifestRecord manifestRecord) {
        this.manifestRecord = manifestRecord;
        this.manifestRecord.addLogEntry(this);
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

    public enum Type {ERROR, WARNING, INFO}
}
