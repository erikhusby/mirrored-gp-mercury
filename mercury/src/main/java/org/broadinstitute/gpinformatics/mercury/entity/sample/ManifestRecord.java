package org.broadinstitute.gpinformatics.mercury.entity.sample;


import com.google.common.base.Function;
import com.google.common.collect.Maps;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A manifest record represents the accumulated data of one row in a sample manifest derived from the sample
 * registration process.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "MANIFEST_RECORD")
public class ManifestRecord {

    @Id
    @Column(name = "MANIFEST_RECORD_ID")
    @SequenceGenerator(name = "SEQ_MANIFEST_RECORD", schema = "mercury", sequenceName = "SEQ_MANIFEST_RECORD")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_RECORD")
    /** JPA ID field */
    @SuppressWarnings("UnusedDeclaration")
    private Long manifestRecordId;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "manifest_record_metadata", schema = "mercury",
            joinColumns = @JoinColumn(name = "MANIFEST_RECORD_ID"),
            inverseJoinColumns = @JoinColumn(name = "METADATA_ID"))
    private Set<Metadata> metadata = new HashSet<>();

    @Transient
    private Map<Metadata.Key, Metadata> metadataMap;

    /**
     * Since a record cannot exist without a successful upload of a manifest, this is the default status state
     */
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

    public ManifestRecord(Metadata... metadata) {
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
            metadataMap = new HashMap<>(Maps.uniqueIndex(metadata, new Function<Metadata, Metadata.Key>() {
                @Override
                public Metadata.Key apply(Metadata metadata) {
                    return metadata.getKey();
                }
            }));
        }
        return metadataMap;
    }

    public Set<Metadata> getMetadata() {
        return this.metadata;
    }

    public Metadata getMetadataByKey(Metadata.Key key) {
        return getMetadataMap().get(key);
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setErrorStatus(ErrorStatus errorStatus) {
        this.errorStatus = errorStatus;
    }

    void setSession(ManifestSession session) {
        this.session = session;
    }

    /**
     * Status represents the states that a manifest record can be in during the registration workflow.
     */
    public enum Status {UPLOADED, ABANDONED, UPLOAD_ACCEPTED, SCANNED, REGISTERED, ACCESSIONED}

    /**
     * Represents the different error states that can occur to a record during the registration process.  For the most
     * part, the existence of an error will halt the registration process for a manifest record, unless it specifically
     * states that the process can continue.
     *
     */
    public enum ErrorStatus {
        /** At some time before the current sample was scanned, another with the exact same
         * sample id and also connected to the current research project was scanned
         */
        DUPLICATE_SAMPLE_ID ,
        /** Another record in the system associated with the same research project and same patient
         * ID has a different value for gender
         */
        MISMATCHED_GENDER,
        /** Another record within this manifest, with the same patient ID has the same value
         * for the tumor/normal indicator
         */
        UNEXPECTED_TUMOR_OR_NORMAL,
        /** This cannot directly apply to an actual record.  Represents a sample tube that is
         * received for which there is no previously uploaded manifest record
         */
        NOT_IN_MANIFEST,
        DUPLICATE_SAMPLE_SCAN,
        /** Represents a scenario in which a record exists that, as of the completion of a session,
         * there was no physical sample scanned to associate with the record
         */
        MISSING_SAMPLE,
        /** Represents a scenario in which the user attempts to accession a source tube that
         * did not make it to the REGISTERED state
         */
        NOT_READY_FOR_ACCESSIONING,
        /** Helpful message to note that the user is attempting to accession a source tube
         * a target vessel that has already gone through accessioning
         */
        ALREADY_SCANNED_TARGET,
        NOT_REGISTERED,
        /** Helpful message to note that the user is attempting to accession a source tube into
         * that has already gone through accessioning
         */
        ALREADY_SCANNED_SOURCE
    }
}
