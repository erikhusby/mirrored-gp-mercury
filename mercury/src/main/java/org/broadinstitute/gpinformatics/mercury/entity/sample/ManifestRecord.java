package org.broadinstitute.gpinformatics.mercury.entity.sample;


import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.hibernate.envers.Audited;
import org.jvnet.inflector.Noun;

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
import java.util.Collection;
import java.util.Collections;
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
    @JoinColumn(name = "MANIFEST_SESSION_ID", insertable = false, updatable = false)
    private ManifestSession manifestSession;

    /**
     * The index of this ManifestRecord within the ManifestSession, this is referenced by the containing ManifestSession
     * to preserve the order of ManifestRecords to facilitate review between manifest upload and acceptance.
     * This index value relates to the row number of the corresponding record in the manifest spreadsheet, but while the
     * spreadsheet data rows start at 2 (there is a single header row), these indexes start at 0.  The deprecated
     * Hibernate IndexColumn annotation (now removed in the latest version of Hibernate) used to allow for specification
     * of a base value for this index, but JPA requires ordered elements to start with an index of 0.
     *
     * {@see #getSpreadsheetRowNumber}
     */
    @Column(name = "MANIFEST_RECORD_INDEX", insertable = false, updatable = false, nullable = false)
    private Integer manifestRecordIndex;

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
            updateMetadataMap();
        }
        return metadataMap;
    }

    private void updateMetadataMap() {
        metadataMap = Maps.uniqueIndex(getMetadata(), new Function<Metadata, Metadata.Key>() {
            @Override
            public Metadata.Key apply(Metadata metadata) {
                return metadata.getKey();
            }
        });
    }

    public Set<Metadata> getMetadata() {
        return this.metadata;
    }

    public Metadata getMetadataByKey(Metadata.Key key) {
        return getMetadataMap().get(key);
    }

    public String getValueByKey(Metadata.Key key) {
        Metadata metadata = getMetadataByKey(key);
        if (metadata != null) {
            return metadata.getValue();
        }
        return null;
    }

    public void addMetadata(Metadata.Key key, String value) {
        Metadata metadata = getMetadataByKey(key);

        if (metadata != null && StringUtils.isNotEmpty(metadata.getValue())) {
            throw new InformaticsServiceException(key.getDisplayName() + (manifestSession == null ?
                    (" is already set to " + metadata.getValue()) :
                    (" is already set for the record for [" + getSampleId() +"]")));
        }

        if(metadata != null) {
            this.metadata.remove(metadata);
        }
        this.metadata.add(new Metadata(key, value));
        updateMetadataMap();
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

    @Override
    public UpdateData getUpdateData() {
        return updateData;
    }

    /**
     * Scan the sample corresponding to this ManifestRecord as part of accessioning.
     * @param key
     * @param value
     */
    public void accessionScan(Metadata.Key key, String value) {
        if (isQuarantined()) {
            throw new InformaticsServiceException(
                    ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(key, value));
        }
        if (status == Status.SCANNED || status == Status.SAMPLE_TRANSFERRED_TO_TUBE) {
            throw new InformaticsServiceException(
                    ErrorStatus.DUPLICATE_SAMPLE_SCAN.formatMessage(key, value));
        }

        status = (manifestSession.getAccessioningProcessType() == ManifestSessionEjb.AccessioningProcessType.COVID)
                ?Status.ACCESSIONED:Status.SCANNED;
    }

    public void setManifestRecordIndex(int manifestRecordIndex) {
        this.manifestRecordIndex = manifestRecordIndex;
    }

    /**
     * Build an error message for duplicate or gender mismatched samples.  All records in {@code conflictingRecords}
     * are expected to belong to the same manifest session.
     */
    public String buildMessageForConflictingRecords(Collection<ManifestRecord> conflictingRecords) {
        String otherSessionName = getOtherSessionName(conflictingRecords);
        StringBuilder messageBuilder = new StringBuilder();

        // Describe how many duplicates were found in a particular manifest session.
        int numInstances = conflictingRecords.size();
        messageBuilder.append(numInstances).append(" ");
        messageBuilder.append(Noun.pluralOf("instance", numInstances));
        messageBuilder.append(" found at ");
        messageBuilder.append(Noun.pluralOf("row", numInstances));
        messageBuilder.append(" ");

        // Collect the spreadsheet row numbers at which the duplicates can be found.
        List<Integer> rowNumbers = new ArrayList<>();
        for (ManifestRecord manifestRecord : conflictingRecords) {
            rowNumbers.add(manifestRecord.getSpreadsheetRowNumber());
        }
        // Sort the spreadsheet row numbers, join with commas and append to the message string.
        Collections.sort(rowNumbers);
        messageBuilder.append(StringUtils.join(rowNumbers, ", "));

        messageBuilder.append(" of ");
        String thisSessionName = getManifestSession().getSessionName();
        messageBuilder.append(
                otherSessionName.equals(thisSessionName) ? "this manifest session" :
                        "manifest session '" + otherSessionName + "'");
        return messageBuilder.toString();
    }

    /**
     * All specified records should be part of the same manifest session, extract the manifest session name from one
     * of them.
     */
    private String getOtherSessionName(Collection<ManifestRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            throw new InformaticsServiceException("records expected to be non-empty");
        }
        return records.iterator().next().getManifestSession().getSessionName();
    }

    /**
     * Create a presentable description of where the specified manifest records conflicting with this record
     * can be found.
     *
     * @param allConflictingRecords  All ManifestRecords matching this ManifestRecord (same patient ID or sample ID,
     *                             depending on the validation being performed).
     */
    private String describeOtherManifestSessionsWithMatchingRecords(Collection<ManifestRecord> allConflictingRecords) {

        // Filter 'thisRecord' from consideration as being in conflict with itself.
        Iterable<ManifestRecord> allButThisRecord = Iterables.filter(allConflictingRecords, new Predicate<ManifestRecord>() {
            @Override
            public boolean apply(@Nullable ManifestRecord record) {
                return record != ManifestRecord.this;
            }
        });

        // Group manifest records by manifest session name.
        ImmutableListMultimap<String, ManifestRecord> recordsBySessionName =
                Multimaps.index(allButThisRecord, new Function<ManifestRecord, String>() {
                    @Override
                    public String apply(ManifestRecord record) {
                        return record.getManifestSession().getSessionName();
                    }
                });

        List<String> messages = new ArrayList<>();
        // Add an appropriate message for each record to messages.
        for (Collection<ManifestRecord> conflictingRecords : recordsBySessionName.asMap().values()) {
            messages.add(buildMessageForConflictingRecords(conflictingRecords));
        }

        // Join the messages for all the manifests containing conflicting records.
        return StringUtils.join(messages, ", ") + ".";
    }

    /**
     * Build an error message for manifest records where the sample id specified is duplicated
     * across one or more records within the same research project.
     */
    public String buildMessageForDuplicateSamples(Collection<ManifestRecord> allRecordsWithTheSameSampleId) {
        String duplicateSamplesMessage =
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(Metadata.Key.SAMPLE_ID, getValueByKey(
                        Metadata.Key.SAMPLE_ID));
        String sessionsWithDuplicates = describeOtherManifestSessionsWithMatchingRecords(allRecordsWithTheSameSampleId);
        return buildSpreadsheetRowMessage() + duplicateSamplesMessage + "  " + sessionsWithDuplicates;
    }

    /**
     * Build a simple spreadsheet row message prefix.
     */
    private String buildSpreadsheetRowMessage() {
        return "At  manifest row "+getManifestRecordIndex()+" : ";
    }

    /**
     * Build an error message for manifest records where the gender specified for a particular patient ID is mismatched
     * across one or more records within the same research project.
     */
    public String buildMessageForMismatchedGenders(Collection<ManifestRecord> allRecordsWithSamePatientId) {
        String mismatchedGendersMessage =
                ErrorStatus.MISMATCHED_GENDER.formatMessage(Metadata.Key.PATIENT_ID, getValueByKey(
                        Metadata.Key.PATIENT_ID));
        String sessionsWithMismatchedGenders = describeOtherManifestSessionsWithMatchingRecords(allRecordsWithSamePatientId);
        return buildSpreadsheetRowMessage() + mismatchedGendersMessage + "  " + sessionsWithMismatchedGenders;
    }

    public String buildMessageForDuplicateMatrixIds(Collection<ManifestRecord> value) {
        String duplicateMatrixIdMessage =
                ErrorStatus.DUPLICATE_MATRIX_ID.formatMessage(Metadata.Key.BROAD_2D_BARCODE, getValueByKey(
                        Metadata.Key.BROAD_2D_BARCODE));
        return buildSpreadsheetRowMessage() + duplicateMatrixIdMessage;
    }

    /**
     * Status represents the states that a manifest record can be in during the registration workflow.
     */
    public enum Status implements Displayable{
        UPLOADED("Uploaded"),
        ABANDONED("Abandoned"),
        UPLOAD_ACCEPTED("Upload Accepted"),
        SCANNED("Scanned"),
        ACCESSIONED("Accessioned"),
        SAMPLE_TRANSFERRED_TO_TUBE("Sample has been transferred");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public static Status fromName(String Name) {

            // todo jmt improve
            Status foundStatus = null;

            for (Status key : values()) {
                if (key.name().equals(Name)) {
                    foundStatus = key;
                    break;
                }
            }
            return foundStatus;
        }

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
        DUPLICATE_SAMPLE_ID("The specified sample ID is duplicated within this Research Project.",
                ManifestEvent.Severity.QUARANTINED),
        /**
         * Another record in the system associated with the same research project and same patient
         * ID has a different value for gender.
         */
        MISMATCHED_GENDER("At least one other manifest entry with the same patient ID has a different gender.",
                ManifestEvent.Severity.ERROR),
        /**
         * Another record in the uploaded manifest has the same 2d Matrix Barcode.
         */
        DUPLICATE_MATRIX_ID("The specified matrix ID is duplicated within this manifest.",
                ManifestEvent.Severity.ERROR),
        /**
         * Another record in the uploaded manifest has the same 2d Matrix Barcode.
         */
        MISSING_MATRIX_IDS("Some manifest Records are not assigned Matrix IDs", ManifestEvent.Severity.ERROR),
        /**
         * Another record in the uploaded manifest has the same 2d Matrix Barcode.
         */
        MISSING_MATRIX_ID("This manifest Record does not have a matrix ID assigned to it", ManifestEvent.Severity.ERROR),
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
        NOT_IN_MANIFEST("The scanned source sample is not found in this manifest.", ManifestEvent.Severity.ERROR),
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
        INVALID_TARGET("The target sample or vessel is invalid.", ManifestEvent.Severity.ERROR),
        MISMATCHED_TARGET("The target sample or vessel does not match the one in the the manifest.", ManifestEvent.Severity.ERROR),
        SOURCE_ALREADY_TRANSFERRED("The source sample has already been transferred to a tube",
                ManifestEvent.Severity.ERROR),
        NO_TRANSFER_ACTION_FOUND("At this time, there was no sample transfer action taken on this tube", ManifestEvent.Severity.WARNING);

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
        Metadata sampleMetadata =
                (manifestSession.isFromSampleKit())?getMetadataByKey(Metadata.Key.BROAD_SAMPLE_ID):getMetadataByKey(Metadata.Key.SAMPLE_ID);
        if (sampleMetadata != null) {
            return sampleMetadata.getValue();
        }
        return null;
    }

    /**
     * Return the spreadsheet row number of the record corresponding to this {@code ManifestRecord}.
     */
    public int getSpreadsheetRowNumber() {
        final int INDEX_TO_SPREADSHEET_ROW_NUMBER_CONVERSION = 2;
        return manifestRecordIndex + INDEX_TO_SPREADSHEET_ROW_NUMBER_CONVERSION;
    }

    public Integer getManifestRecordIndex() {
        return manifestRecordIndex;
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

