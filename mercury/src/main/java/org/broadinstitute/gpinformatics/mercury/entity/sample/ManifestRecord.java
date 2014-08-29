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

    @OneToMany(cascade = {CascadeType.PERSIST}, mappedBy = "manifestRecord")
    private List<ManifestEvent> logEntries = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession session;

    /**
     * For JPA
     */
    protected ManifestRecord() {}

    public ManifestRecord(ManifestSession session, Metadata... metadata) {
        this.session = session;
        this.session.addRecord(this);
        this.metadata.addAll(Arrays.asList(metadata));
    }

    /**
     * Builds the Metadata Map if it has not already been built.
     */
    private Map<Metadata.Key, Metadata> getMetadataMap() {
        // This is constructed lazily as it can't be built within the no-arg constructor since the 'metadata' field
        // upon which it depends will not have been initialized.
        if (metadataMap == null) {
            metadataMap = new HashMap<>(Maps.uniqueIndex(getMetadata(), new Function<Metadata, Metadata.Key>() {
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

    public Metadata getMetadataByKey(Metadata.Key sampleId) {
        return getMetadataMap().get(sampleId);
    }

    public Status getStatus() {
        return status;
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

    public boolean fatalErrorExists() {
        boolean fatality = false;

        for (ManifestEvent logEntry : logEntries) {
            if (logEntry.getLogType() == ManifestEvent.Type.FATAL) {
                fatality = true;
                break;
            }
        }

        return fatality;
    }

    public Set<String> getFatalRecordMessages() {
        Set<String> fatalMessages = new HashSet<>();
        for (ManifestEvent manifestEvent : getLogEntries()) {
            if (manifestEvent.getLogType() == ManifestEvent.Type.FATAL) {
                fatalMessages.add(manifestEvent.getMessage());
            }
        }
        return fatalMessages;
    }

    /**
     * Status represents the states that a manifest record can be in during the registration workflow.
     */
    public enum Status {
        UPLOADED, ABANDONED, UPLOAD_ACCEPTED, SCANNED, REGISTERED, ACCESSIONED
    }

    /**
     * Represents the different error states that can occur to a record during the registration process.  For the most
     * part, the existence of an error will halt the registration process for a manifest record, unless it specifically
     * states that the process can continue.
     */
    public enum ErrorStatus {
        /**
         * At some time before the current sample was scanned, another with the exact same
         * sample id and also connected to the current research project was scanned.
         */
        DUPLICATE_SAMPLE_ID("The given sample ID is a duplicate of another."),
        /**
         * Another record in the system associated with the same research project and same patient
         * ID has a different value for gender.
         */
        MISMATCHED_GENDER("At least one other manifest entry with the same patient ID has a different gender"),
        /**
         *
         * TODO not sure this is an error, should be tracked by a Decision.
         *
         * Another record within this manifest, with the same patient ID has the same value
         * for the tumor/normal indicator.
         */
        UNEXPECTED_TUMOR_OR_NORMAL(
                "At least one other manifest entry with the same patient ID has a different indicator for tumor/normal"),
        /**
         * This cannot directly apply to an actual record.  Represents a sample tube that is
         * received for which there is no previously uploaded manifest record.
         */
        NOT_IN_MANIFEST("The scanned sample is not found in any manifest"),
        /**
         * TODO This seems to be a duplicate of DUPLICATE_SAMPLE_ID.  Need to fully define what this error case means.
         */
        DUPLICATE_SAMPLE_SCAN(" "),
        /**
         * No sample was scanned for a manifest record.
         */
        MISSING_SAMPLE("No sample has been scanned to correspond with the manifest record"),
        /**
         * Represents a scenario in which the user attempts to accession a source tube that
         * did not make it to the REGISTERED state.
         */
        NOT_READY_FOR_ACCESSIONING("Attempting to accession a sample that has not completed manifest registration"),
        /**
         * Helpful message to note that the user is attempting to accession a source tube into
         * a target vessel that has already gone through accessioning.
         */
        ALREADY_SCANNED_TARGET("The scanned target tube has already been associated with another source sample"),
        /**
         * TODO This seems to be a duplicate of NOT_READY_FOR_ACCESSIONING.  Need to fully define what this case means.
         */
        NOT_REGISTERED(" "),
        /**
         * Helpful message to note that the user is attempting to accession a source tube into
         * that has already gone through accessioning.
         */
        ALREADY_SCANNED_SOURCE("The scanned source tube has already been through the accessioning process"),

        PREVIOUS_ERRORS_UNABLE_TO_CONTINUE("Due to Errors previously found, this sample is unable to continue.");
        private String message;

        ErrorStatus(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public String formatMessage(String entityType, String value) {
            return String.format("For %s %s: %s", entityType, value, getMessage());
        }
    }
}
