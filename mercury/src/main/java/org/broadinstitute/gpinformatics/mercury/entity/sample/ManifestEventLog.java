package org.broadinstitute.gpinformatics.mercury.entity.sample;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ManifestEventLog {


    private String message;
    private ManifestRecord manifestRecord;
    private Type logType;

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
}
