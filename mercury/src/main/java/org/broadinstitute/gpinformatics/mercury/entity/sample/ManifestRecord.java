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

    @Column(name = "MANIFEST_RECORD_ID")
    @Id
    private Long manifestRecordId;

    private Map<Metadata.Key,Metadata> metadata = new HashMap<>();

    private Status status;

    private ErrorStatus errorStatus;

    // TODO is this really one to many?
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "manifestRecord")
    private List<ManifestEvent> logEntries = new ArrayList<>();

    /** For JPA */
    protected ManifestRecord() {
    }

    public ManifestRecord(List<Metadata> metadata) {
        this(metadata, Status.UPLOADED, null);
    }

    public ManifestRecord(List<Metadata> metadata, Status status, ErrorStatus errorStatus) {
        this.metadata = new HashMap<>(Maps.uniqueIndex(metadata, new Function<Metadata, Metadata.Key>() {
            @Override
            public Metadata.Key apply(Metadata metadata) {
                return metadata.getKey();
            }
        }));
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

    public List<ManifestEvent> getLogEntries() {
        return logEntries;
    }

    public void addLogEntry(ManifestEvent manifestEvent) {
        this.logEntries.add(manifestEvent);
    }

    public enum Status {ABANDONED, UPLOADED}

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum ErrorStatus {
        DUPLICATE_SAMPLE_ID
    }
}
