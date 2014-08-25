package org.broadinstitute.gpinformatics.mercury.entity.sample;

import clover.com.google.common.base.Function;
import clover.com.google.common.collect.Maps;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Entity
@Audited
@Table(schema = "mercury")
public class ManifestRecord {

    @Column(name = "id")
    @Id
    private Long id;

    private Map<Metadata.Key,Metadata> metadata = new HashMap<>();

    private Status status = Status.UPLOADED;

    private ErrorStatus errorStatus;

    // TODO is this really one to many?
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "manifestRecord")
    private List<ManifestEventLog> logEntries = new ArrayList<>();

    /** For JPA */
    protected ManifestRecord() {
    }

    public ManifestRecord(List<Metadata> metadata) {
        this.metadata = new HashMap<>(Maps.uniqueIndex(metadata, new Function<Metadata, Metadata.Key>() {
            @Override
            public Metadata.Key apply(Metadata metadata) {
                return metadata.getKey();
            }
        }));
    }

    public ManifestRecord(List<Metadata> metadata, Status status, ErrorStatus errorStatus) {
        this(metadata);
        this.status = status;
        this.errorStatus = errorStatus;
    }

    public Metadata getField(Metadata.Key sampleId) {


        return metadata.get(sampleId);
    }

    public Status getStatus() {
        return status;
    }

    public ErrorStatus getErrorStatus() {
        return errorStatus;
    }

    public List<ManifestEventLog> getLogEntries() {
        return logEntries;
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
