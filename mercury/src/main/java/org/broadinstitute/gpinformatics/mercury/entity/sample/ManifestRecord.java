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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
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

    @Id
    @Column(name = "MANIFEST_RECORD_ID")
    @SequenceGenerator(name = "SEQ_MANIFEST_RECORD", schema = "mercury", sequenceName = "SEQ_MANIFEST_RECORD")
    @GeneratedValue
    private Long manifestRecordId;

    @OneToMany
    @JoinTable(name = "manifest_record_metadata",
            joinColumns = @JoinColumn(name = "MANIFEST_RECORD_ID", referencedColumnName = "manifest_record_id"))
    @MapKeyEnumerated(EnumType.STRING)
    private Map<Metadata.Key, Metadata> metadata = new HashMap<>();

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private ErrorStatus errorStatus;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "manifestRecord")
    private List<ManifestEvent> logEntries = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession session;

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

    public ManifestSession getSession() {
        return session;
    }

    public void setSession(ManifestSession session) {
        this.session = session;
    }

    public enum Status {ABANDONED, UPLOADED}

    /**
     * TODO scottmat fill in javadoc!!!
     */
    public static enum ErrorStatus {
        DUPLICATE_SAMPLE_ID
    }
}
