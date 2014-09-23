package org.broadinstitute.gpinformatics.mercury.entity.sample;


import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A manifest record represents the accumulated data of one row in a sample manifest derived from the sample
 * registration process.
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(schema = "mercury", name = "MANIFEST_RECORD")
public class ManifestRecord implements Updatable {

    @Id
    @Column(name = "MANIFEST_RECORD_ID")
    @SequenceGenerator(name = "SEQ_MANIFEST_RECORD", schema = "mercury", sequenceName = "SEQ_MANIFEST_RECORD")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_RECORD")
    /** JPA ID field */
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

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "manifestRecord", orphanRemoval = true)
    private List<ManifestEvent> manifestEvents = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "manifest_session_id")
    private ManifestSession manifestSession;

    @Embedded
    private UpdateData updateData = new UpdateData();

    /**
     * For JPA.
     */
    protected ManifestRecord() {
    }

    public ManifestRecord(Metadata... metadata) {
        this.metadata.addAll(Arrays.asList(metadata));
    }

    /**
     * Builds the Metadata Map if it has not already been built.
     */
    private Map<Metadata.Key, Metadata> getMetadataMap() {
        // This is constructed lazily as it can't be built within the no-arg constructor since the 'metadata' field
        // upon which it depends will not have been initialized.
        if (metadataMap == null) {
            metadataMap = Maps.uniqueIndex(getMetadata(), new Function<Metadata, Metadata.Key>() {
                @Override
                public Metadata.Key apply(Metadata metadata) {
                    return metadata.getKey();
                }
            });
        }
        return metadataMap;
    }

    public Set<Metadata> getMetadata() {
        return this.metadata;
    }

    public Metadata getMetadataByKey(Metadata.Key key) {
        return getMetadataMap().get(key);
    }

    public String getValueByKey(Metadata.Key key) {
        return getMetadataByKey(key).getValue();
    }

    public Status getStatus() {
        return status;
    }

    public List<ManifestEvent> getManifestEvents() {
        return manifestEvents;
    }

    public void addManifestEvent(ManifestEvent manifestEvent) {
        this.manifestEvents.add(manifestEvent);
    }

    public ManifestSession getManifestSession() {
        return manifestSession;
    }

    void setManifestSession(ManifestSession manifestSession) {
        this.manifestSession = manifestSession;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    Long getManifestRecordId() {
        return manifestRecordId;
    }

    public boolean isQuarantined() {
        for (ManifestEvent manifestEvent : manifestEvents) {
            if (manifestEvent.getSeverity() == ManifestEvent.Severity.QUARANTINED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scan the sample corresponding to this ManifestRecord as part of accessioning.
     */
    public void accessionScan() {

        if (isQuarantined()) {
            throw new InformaticsServiceException(
                    ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(Metadata.Key.SAMPLE_ID, getSampleId()));
        }
        if (status == Status.SCANNED) {
            throw new InformaticsServiceException(
                    ErrorStatus.DUPLICATE_SAMPLE_SCAN.formatMessage(Metadata.Key.SAMPLE_ID, getSampleId()));
        }

        status = Status.SCANNED;
    }

    /**
     * Status represents the states that a manifest record can be in during the registration workflow.
     */
    public enum Status {
        UPLOADED, ABANDONED, UPLOAD_ACCEPTED, SCANNED, ACCESSIONED, SAMPLE_TRANSFERRED_TO_TUBE
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
        DUPLICATE_SAMPLE_ID("The given sample ID is a duplicate of another.", ManifestEvent.Severity.QUARANTINED),
        /**
         * Another record in the system associated with the same research project and same patient
         * ID has a different value for gender.
         */
        MISMATCHED_GENDER("At least one other manifest entry with the same patient ID has a different gender.",
                ManifestEvent.Severity.ERROR),
        /**
         * TODO not sure this is an error, should be tracked by a Decision.
         * <p/>
         * Another record within this manifest, with the same patient ID has the same value
         * for the tumor/normal indicator.
         */
        UNEXPECTED_TUMOR_OR_NORMAL(
                "At least one other manifest entry with the same patient ID has a different indicator for tumor/normal.",
                ManifestEvent.Severity.ERROR),
        /**
         * This cannot directly apply to an actual record.  Represents a sample tube that is
         * received for which there is no previously uploaded manifest record.
         */
        NOT_IN_MANIFEST("The scanned sample is not found in any manifest.", ManifestEvent.Severity.ERROR),
        /**
         * Encapsulates the error message to indicate to the user that they have already scanned the tube
         */
        DUPLICATE_SAMPLE_SCAN("This sample has been scanned previously.", ManifestEvent.Severity.ERROR),
        /**
         * No sample was scanned for a manifest record.
         */
        MISSING_SAMPLE("No sample has been scanned to correspond with the manifest record.",
                ManifestEvent.Severity.QUARANTINED),
        /**
         * Represents a scenario in which the user attempts to transfer a source tube that
         * did not make it to the ACCESSIONED state.
         */
        NOT_READY_FOR_TUBE_TRANSFER("Attempting to transfer a sample that has not completed accessioning.",
                ManifestEvent.Severity.QUARANTINED),
        /**
         * Helpful message to note that the user is attempting to accession a source tube into
         * a target vessel that has already gone through accessioning.
         */
        ALREADY_SCANNED_TARGET("The scanned target tube has already been associated with another source sample.",
                ManifestEvent.Severity.ERROR),
        /**
         * TODO This seems to be a duplicate of NOT_READY_FOR_TUBE_TRANSFER.  Need to fully define what this case means.
         */
        NOT_REGISTERED(" ", ManifestEvent.Severity.ERROR),
        /**
         * Helpful message to note that the user is attempting to accession a source tube
         * that has already gone through accessioning.
         */
        ALREADY_SCANNED_SOURCE("The scanned source tube has already been through the accessioning process.",
                ManifestEvent.Severity.ERROR),

        PREVIOUS_ERRORS_UNABLE_TO_CONTINUE("Due to errors previously found, this sample is unable to continue.",
                ManifestEvent.Severity.ERROR),
        INVALID_TARGET("The Target sample or vessel appears to be invalid.", ManifestEvent.Severity.ERROR);

        private final String baseMessage;
        private final ManifestEvent.Severity severity;

        ErrorStatus(String baseMessage, ManifestEvent.Severity severity) {

            this.baseMessage = baseMessage;
            this.severity = severity;
        }

        /**
         * The base message to which an entity type and value will be added for display.
         */
        public String getBaseMessage() {
            return baseMessage;
        }

        public String formatMessage(Metadata.Key key, String value) {
            return formatMessage(key.getDisplayName(), value);
        }

        public ManifestEvent.Severity getSeverity() {
            return severity;
        }

        public String formatMessage(String keyString, String value) {
            return String.format("For %s %s: %s", keyString, value, baseMessage);
        }
    }

    @Nullable public String getSampleId() {
        Metadata sampleMetadata = getMetadataByKey(Metadata.Key.SAMPLE_ID);
        if (sampleMetadata != null) {
            return sampleMetadata.getValue();
        }
        return null;
    }

    public UpdateData getUpdateData() {
        return updateData;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("manifestRecordId", manifestRecordId)
                .append("collaboratorSampleId", getSampleId())
                .append("status", status)
                .toString();
    }
}
