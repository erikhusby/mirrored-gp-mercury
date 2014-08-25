package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Entity
@Audited
@Table(schema = "mercury")
public class ManifestEventLog {

    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_EVENT_LOG", schema = "mercury",sequenceName = "SEQ_MANIFEST_EVENT_LOG")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_EVENT_LOG")
    private Long manifestEventLogId;

    private String message;

    @ManyToOne(optional = true)
    private ManifestRecord manifestRecord;

    private Type logType;

    /** For JPA */
    public ManifestEventLog() {
    }

    public ManifestEventLog(String message, Type logType) {

        this.message = message;
        this.logType = logType;
    }

    public ManifestEventLog(String message, ManifestRecord record, Type logType) {

        this.message = message;
        setManifestRecord(record);
        this.logType = logType;
    }

    public void setManifestRecord(ManifestRecord manifestRecord) {
        this.manifestRecord = manifestRecord;
        this.manifestRecord.addLogEntry(this);
    }

    public enum Type {ERROR}

    public ManifestRecord getManifestRecord() {
        return manifestRecord;
    }
}
