package org.broadinstitute.gpinformatics.mercury.entity.sample;

import clover.com.google.common.base.Function;
import clover.com.google.common.collect.Maps;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Entity
@Audited
@Table(schema = "mercury", name = "MANIFEST_RECORD")
public class ManifestRecord {

    @Id
    @Column(name = "MANIFEST_RECORD_ID")
    @SequenceGenerator(name = "SEQ_MANIFEST_RECORD", schema = "mercury", sequenceName = "SEQ_MANIFEST_RECORD")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_RECORD")
    private Long manifestRecordId;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "manifest_record_metadata", schema = "mercury",
            joinColumns = @JoinColumn(name = "MANIFEST_RECORD_ID"),
            inverseJoinColumns = @JoinColumn(name = "METADATA_ID"))
    private List<Metadata> metadata = new ArrayList<>();

    @Transient
    private Map<Metadata.Key, Metadata> metadataMap;

    @Enumerated(EnumType.STRING)
    private Status status = Status.UPLOADED;

    @Enumerated(EnumType.STRING)
    private ErrorStatus errorStatus;

    @OneToMany(cascade = {CascadeType.PERSIST}, mappedBy = "manifestRecord")
    private List<ManifestEvent> logEntries = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession session;

    /**
     * For JPA
     */
    protected ManifestRecord() {}

    public ManifestRecord(Metadata...metadata) {
        this(null, metadata);
    }

    public ManifestRecord(ErrorStatus errorStatus, Metadata... metadata) {
        this.metadata.addAll(Arrays.asList(metadata));
        this.errorStatus = errorStatus;
    }

    /**
     * Builds the Metadata Map if it has not already been built.
     */
    private Map<Metadata.Key, Metadata> getMetadataMap() {
        // This is constructed lazily as it can't be built within the no-arg constructor since the 'metadata' field
        // upon which it depends will not have been initialized.
        if (metadataMap == null) {
            this.metadataMap = new HashMap<>(Maps.uniqueIndex(metadata, new Function<Metadata, Metadata.Key>() {
                @Override
                public Metadata.Key apply(Metadata metadata) {
                    return metadata.getKey();
                }
            }));
        }
        return metadataMap;
    }

    public List<Metadata> getMetadata() {
        return this.metadata;
    }

    public Metadata getMetadataByKey(Metadata.Key sampleId) {
        return getMetadataMap().get(sampleId);
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

    public ManifestSession getSession() {
        return session;
    }

    public void setSession(ManifestSession session) {
        this.session = session;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setErrorStatus(ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    public enum Status {UPLOADED, ABANDONED, UPLOAD_ACCEPTED, SCANNED, REGISTERED, ACCESSIONED}

    public enum ErrorStatus {
        DUPLICATE_SAMPLE_ID, MISMATCHED_GENDER, UNEXPECTED_TUMOR_OR_NORMAL, NOT_IN_MANIFEST, DUPLICATE_SAMPLE_SCAN,
        MISSING_SAMPLE, CONTAINER_LOST, CONTAINER_DAMAGED, NOT_READY_FOR_ACCESSIONING, ALREADY_SCANNED_TARGET,
        NOT_REGISTERED, ALREADY_SCANNED_SOURCE
    }
}
