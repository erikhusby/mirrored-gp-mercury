package org.broadinstitute.gpinformatics.mercury.entity.sample;

import clover.com.google.common.base.Function;
import clover.com.google.common.collect.Maps;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ManifestRecord {


    private Map<Metadata.Key,Metadata> metadata = new HashMap<>();
    private Status status = Status.UPLOADED;
    private ErrorStatus errorStatus;
    private List<ManifestEventLog> logEntries = new ArrayList<>();

    public ManifestRecord(List<Metadata> metadata) {

        this.metadata = new HashMap<>(Maps.uniqueIndex(metadata, new Function<Metadata, Metadata.Key>() {
            @Override
            public Metadata.Key apply(@Nullable Metadata metadata) {
                return metadata.getKey();
            }
        }));

    }

    public Metadata getField(Metadata.Key sampleId) {


        return metadata.get(sampleId);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ErrorStatus getErrorStatus() {
        return errorStatus;
    }

    public void setErrorStatus(ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    public List<ManifestEventLog> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(List<ManifestEventLog> logEntries) {
        this.logEntries = logEntries;
    }

    public void addLogEntry(ManifestEventLog manifestEventLog) {
        this.logEntries.add(manifestEventLog);

    }

    public enum Status {ABANDONED, UPLOADED}

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum ErrorStatus {
        DUPLICATE_SAMPLE_ID
    }
}
