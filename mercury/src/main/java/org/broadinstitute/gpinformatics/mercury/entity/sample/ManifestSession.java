package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import javax.annotation.Nonnull;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manifest session contains the information related to a single manifest upload during the sample accessioning process.
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(schema = "mercury", name = "MANIFEST_SESSION")
public class ManifestSession implements Updatable {

    public static final String VESSEL_LABEL = "Vessel barcode";
    public static final String RECEIPT_BSP_USER = "receiptBspUser";

    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_SESSION", schema = "mercury", sequenceName = "SEQ_MANIFEST_SESSION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_SESSION")
    private Long manifestSessionId;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "RESEARCH_PROJECT_ID")
    private ResearchProject researchProject;

    @Column(name = "SESSION_PREFIX")
    private String sessionPrefix;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.OPEN;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "MANIFEST_SESSION_ID", nullable = false)
    @OrderColumn(name = "MANIFEST_RECORD_INDEX", nullable = false)
    // Envers insists on auditing a relation with a specified @OrderColumn in a special table, although this seems
    // redundant to the auditing that is already in place for ManifestRecord.  In the absence of a specified
    // @AuditJoinTable, Envers would use a default join table name that exceeds the Oracle 30 character identifier
    // limit, so this specifies a shorter name.
    @AuditJoinTable(name = "MANIFEST_RECORD_JOIN_AUD")
    private List<ManifestRecord> records = new ArrayList<>();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "manifestSession", orphanRemoval = true)
    private List<ManifestEvent> manifestEvents = new ArrayList<>();

    @Embedded
    private UpdateData updateData = new UpdateData();

    @NotAudited
    @Formula("(select count(*) from mercury.manifest_record where manifest_record.status = 'SAMPLE_TRANSFERRED_TO_TUBE'" +
             " and manifest_record.manifest_session_id = manifest_session_id)")
    private int numberOfTubesTransferred;

    @NotAudited
    @Formula("(select count(*) from mercury.manifest_record rec where rec.manifest_session_id = manifest_session_id)")
    private int totalNumberOfRecords;

    @NotAudited
    @Formula("(select count(*) from mercury.manifest_record record"
             + " join mercury.manifest_event evt on evt.MANIFEST_RECORD_ID = record.MANIFEST_RECORD_ID and evt.SEVERITY = 'QUARANTINED'"
             + " where record.manifest_session_id = manifest_session_id)")
    private int numberOfQuarantinedRecords;

    @Column(name = "FROM_SAMPLE_KIT")
    private boolean fromSampleKit;

    @Column(name = "RECEIPT_TICKET")
    private String receiptTicket;

    /**
     * For JPA.
     */
    protected ManifestSession() {
    }

    public ManifestSession(ResearchProject researchProject, String manifestSessionName, BspUser createdBy,
                           boolean fromSampleKit) {
        this(researchProject, manifestSessionName, createdBy, fromSampleKit, Collections.<ManifestRecord>emptyList());
    }

    public ManifestSession(ResearchProject researchProject, String sessionName, BspUser createdBy, boolean fromSampleKit,
                           Collection<ManifestRecord> manifestRecords) {
        this.researchProject = researchProject;
        if (researchProject != null) {
            researchProject.addManifestSession(this);
        }
        sessionPrefix = sessionName;
        updateData.setCreatedBy(createdBy.getUserId());
        this.fromSampleKit = fromSampleKit;
        if(fromSampleKit) {
            status = SessionStatus.PENDING_SAMPLE_INFO;
        }

        addRecords(manifestRecords);
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public Long getManifestSessionId() {
        return manifestSessionId;
    }

    /**
     * Calculate and return the session name.  Note this is a function of the Hibernate-assigned ID and the result of
     * this method is not cached.  If this is called before this ManifestSession is assigned an ID it will produce a
     * different result than after an ID is assigned.
     */
    public String getSessionName() {
        return sessionPrefix.trim() + "-" + manifestSessionId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public void addRecord(ManifestRecord record) {
        records.add(record);
        record.setManifestSession(this);
        if(record.getManifestRecordIndex() == null) {
            /*  ManifestRecords use a zero-based offset.  Normally the spreadsheet parser assigns these but the
                addition of LabEvents for receipt utilizing this index makes it necessary for us to set this value
                even without spreadsheet upload
                */
            record.setManifestRecordIndex(getRecords().size()-1);
        }
        if(isFromSampleKit()) {
            if(status == SessionStatus.PENDING_SAMPLE_INFO) {
                setStatus(SessionStatus.ACCESSIONING);
            }
            record.setStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);
        }
    }

    public void addRecords(Collection<ManifestRecord> manifestRecords) {
        for (ManifestRecord manifestRecord : manifestRecords) {
            addRecord(manifestRecord);
        }
    }

    public List<ManifestRecord> getRecords() {
        return records;
    }

    public void addManifestEvent(ManifestEvent manifestEvent) {
        manifestEvents.add(manifestEvent);
        manifestEvent.setManifestSession(this);
    }

    public List<ManifestEvent> getManifestEvents() {
        return manifestEvents;
    }

    public boolean isFromSampleKit() {
        return fromSampleKit;
    }

    public String getReceiptTicket() {
        return receiptTicket;
    }

    public void setReceiptTicket(String receiptTicket) {
        this.receiptTicket = receiptTicket;
    }

    /**
     * Called during manifest upload, this executes validations against the records in this manifest (taking into
     * context records from other manifests in the same Research Project) that have not already been validated.
     */
    public void validateManifest() {
        List<ManifestRecord> allManifestRecordsAcrossThisResearchProject = researchProject != null ?
                researchProject.collectNonQuarantinedManifestRecords() : Collections.emptyList();

        validateDuplicateCollaboratorSampleIDs(allManifestRecordsAcrossThisResearchProject);
        validateInconsistentGenders(allManifestRecordsAcrossThisResearchProject);
    }

    /**
     * Encapsulates the logic to validate a given set of manifest records for inconsistent gender values for a
     * patient ID.
     *
     * @param manifestRecords Collection of all manifest records against which to perform
     *                        gender mismatch validation
     */
    private void validateInconsistentGenders(Collection<ManifestRecord> manifestRecords) {

        Multimap<String, ManifestRecord> recordsByPatientId = buildMultimapByKey(
                manifestRecords, Metadata.Key.PATIENT_ID);

        Iterable<Map.Entry<String, Collection<ManifestRecord>>> patientsWithInconsistentGenders =
                filterForPatientsWithInconsistentGenders(recordsByPatientId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : patientsWithInconsistentGenders) {
            for (ManifestRecord record : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (this.equals(record.getManifestSession())) {
                    String message = record.buildMessageForMismatchedGenders(entry.getValue());
                    addManifestEvent(new ManifestEvent(
                            ManifestRecord.ErrorStatus.MISMATCHED_GENDER, message, record));
                }
            }
        }
    }

    /**
     * Helper method for the inconsistent gender validation to limit the final validation logic to just records where
     * the inconsistency is found.
     *
     * @param recordsByPatientId an iterable of the map entries found in the Multimap which grouped manifest records
     *                           by their patient ID
     */
    private Iterable<Map.Entry<String, Collection<ManifestRecord>>> filterForPatientsWithInconsistentGenders(
            Multimap<String, ManifestRecord> recordsByPatientId) {

        return Iterables.filter(recordsByPatientId.asMap().entrySet(),
                new Predicate<Map.Entry<String, Collection<ManifestRecord>>>() {

                    @Override
                    public boolean apply(Map.Entry<String, Collection<ManifestRecord>> entry) {
                        final Set<String> allGenders = new HashSet<>();
                        for (ManifestRecord manifestRecord : entry.getValue()) {
                            allGenders.add(manifestRecord.getValueByKey(Metadata.Key.GENDER));
                            if (allGenders.size() > 1) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    /**
     * Validate a Collection of manifest records for non-unique collaborator sample IDs.
     *
     * @param manifestRecords Collection of all manifest records against which to perform unique sample id
     *                        validation
     */
    private void validateDuplicateCollaboratorSampleIDs(Collection<ManifestRecord> manifestRecords) {

        // Build a map of collaborator sample IDs to manifest records with those collaborator sample IDs.
        Multimap<String, ManifestRecord> recordsBySampleId = buildMultimapBySampleId(manifestRecords);

        // Remove entries in this map which are not duplicates (i.e., leave only the duplicates).
        Iterable<Map.Entry<String, Collection<ManifestRecord>>> filteredDuplicateSamples =
                filterForKeysWithMultipleValues(recordsBySampleId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : filteredDuplicateSamples) {
            for (ManifestRecord duplicatedRecord : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (this.equals(duplicatedRecord.getManifestSession())) {
                    String message = duplicatedRecord.buildMessageForDuplicateSamples(entry.getValue());
                    addManifestEvent(new ManifestEvent(
                            ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, message, duplicatedRecord));
                }
            }
        }
    }

    /**
     * Helper method to filter out entries in a given MultiMap that only have 1 member of the value collection.
     *
     * @param recordsByMetadataValue MultiMap of Manifest Records which are to be filtered
     *
     * @return An iterable minus the entries which had only one entry in the map value
     */
    private Iterable<Map.Entry<String, Collection<ManifestRecord>>> filterForKeysWithMultipleValues(
            Multimap<String, ManifestRecord> recordsByMetadataValue) {

        return Iterables.filter(
                recordsByMetadataValue.asMap().entrySet(),
                new Predicate<Map.Entry<String, Collection<ManifestRecord>>>() {
                    @Override
                    public boolean apply(Map.Entry<String, Collection<ManifestRecord>> entry) {
                        return entry.getValue().size() > 1;
                    }
                });
    }

    /**
     * Helper method to Build a MultiMap of manifest records by a given Metadata Key.
     *
     * @param allEligibleManifestRecords The set of manifest records to be divided up into a MultiMap
     * @param key                        type of Metadata key who's value will be index into the newly created MultiMap
     *
     * @return A MultiMap of manifest records indexed the corresponding value represented by key
     */
    private Multimap<String, ManifestRecord> buildMultimapByKey(
            Collection<ManifestRecord> allEligibleManifestRecords, final Metadata.Key key) {

        return Multimaps.index(allEligibleManifestRecords,
                new Function<ManifestRecord, String>() {
                    @Override
                    public String apply(ManifestRecord manifestRecord) {
                        return manifestRecord.getValueByKey(key);
                    }
                });
    }

    /**
     * Helper method to Build a MultiMap of manifest records by the sampleId.
     *
     * @param manifestRecords The set of manifest records to be divided up into a MultiMap
     *
     * @return A MultiMap of manifest records indexed by the corresponding sampleId value
     */
    private Multimap<String, ManifestRecord> buildMultimapBySampleId(Collection<ManifestRecord> manifestRecords) {
        return Multimaps.index(manifestRecords,
                new Function<ManifestRecord, String>() {
                    @Override
                    public String apply(ManifestRecord manifestRecord) {
                        return manifestRecord.getSampleId();
                    }
                });
    }

    /**
     * Filters out the list of records to return only the ones that do not have blocking errors such as
     * Duplicate_Sample_ID
     *
     * @return all records on this session without blocking errors.
     */
    public List<ManifestRecord> getNonQuarantinedRecords() {
        List<ManifestRecord> allRecords = new ArrayList<>();
        for (ManifestRecord manifestRecord : getRecords()) {
            if (!manifestRecord.isQuarantined()) {
                allRecords.add(manifestRecord);
            }
        }
        return allRecords;
    }

    private Collection<ManifestRecord> getQuarantinedRecords() {
        return CollectionUtils.subtract(getRecords(), getNonQuarantinedRecords());
    }

    /**
     * hasErrors is used to determine if any manifest event entries exist that can be considered errors
     *
     * @return true if even one manifest event entry can be considered an error
     */
    public boolean hasErrors() {
        return ManifestEvent.hasManifestEventOfType(manifestEvents,
                EnumSet.of(ManifestEvent.Severity.ERROR, ManifestEvent.Severity.QUARANTINED));
    }

    /**
     * Method to find the manifest record that has the specified Key value combo as one of it's records.
     */
    public ManifestRecord findRecordByKey(String value, Metadata.Key keyToFindRecordBy)
            throws TubeTransferException {
        List<ManifestRecord> records = findRecordsByKey(value, keyToFindRecordBy);
        if (records.isEmpty()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST, Metadata.Key.SAMPLE_ID, value);
        } else {
            return records.iterator().next();
        }
    }

    /**
     * Method to find the manifest records that have the specified Key value combo as one of it's records.
     * Returns empty list if no matches were found.
     */
    public List<ManifestRecord> findRecordsByKey(String value, Metadata.Key keyToFindRecordBy) {
        return records.stream().
                filter(record -> record.getValueByKey(keyToFindRecordBy).equals(value)).
                collect(Collectors.toList());
    }

    /**
     * Method to support the user accepting an uploaded manifest in order to continue with the accessioning process.
     * To do so, it will set all non quarantined records within this session to the status of UPLOAD_ACCEPTED
     */
    public void acceptUpload() {
        for (ManifestRecord record : getNonQuarantinedRecords()) {
            record.setStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);
        }
        setStatus(SessionStatus.ACCESSIONING);
    }

    /**
     * Creates and returns an object which represents the current state of the session.  A summary of errors found in
     * the associated events, and particular counts of interest are populated in the pojo
     *
     * @return a {@link ManifestStatus} populated with summary information of interest for the session
     */
    public ManifestStatus generateSessionStatusForClose() {

        List<ManifestRecord> nonQuarantinedRecords = getNonQuarantinedRecords();

        Set<String> manifestMessages = new HashSet<>();

        for (ManifestRecord manifestRecord : nonQuarantinedRecords) {
            if (manifestRecord.getStatus() != ManifestRecord.Status.SCANNED) {
                manifestMessages.add(ManifestRecord.ErrorStatus.MISSING_SAMPLE
                        .formatMessage(Metadata.Key.SAMPLE_ID, manifestRecord.getSampleId()));
            }
        }
        int eligibleSize = eligibleRecordsBasedOnStatus(nonQuarantinedRecords, ManifestRecord.Status.UPLOAD_ACCEPTED);

        return new ManifestStatus(getRecords().size(), eligibleSize,
                getRecordsByStatus(ManifestRecord.Status.SCANNED).size(), manifestMessages,
                getQuarantinedRecords().size());
    }

    /**
     * Returns the total number of records that have been transferred to a mercury vessel.
     */
    public int getNumberOfTubesTransferred() {
        return numberOfTubesTransferred;
    }

    /**
     * Returns the total number of records that can be transferred to a mercury vessel.
     */
    public int getNumberOfTubesAvailableForTransfer() {
        return totalNumberOfRecords - numberOfTubesTransferred - numberOfQuarantinedRecords;
    }

    public int getNumberOfQuarantinedRecords() {
        return numberOfQuarantinedRecords;
    }

    /**
     * finds the total number of records that match a given status
     * @param totalRecords          a collection of records within which to search for records of the given status
     * @param statusOfEligibility   status of records to consider in the count
     */
    private static int eligibleRecordsBasedOnStatus(Collection<ManifestRecord> totalRecords,
                                                    ManifestRecord.Status statusOfEligibility) {
        int eligibleSize = 0;

        for (ManifestRecord candidateRecord : totalRecords) {
            if (candidateRecord.getStatus() == statusOfEligibility) {
                eligibleSize++;
            }
        }
        return eligibleSize;
    }

    /**
     * Returns all records contained in this session that currently have a status that matches the given status
     *
     * @param status Status with which to filter records to be returned
     *
     * @return A collection of records filtered by the given status
     */
    private Collection<ManifestRecord> getRecordsByStatus(ManifestRecord.Status status) {

        List<ManifestRecord> foundRecords = new ArrayList<>();

        for (ManifestRecord record : records) {
            if (record.getStatus() == status) {
                foundRecords.add(record);
            }
        }

        return foundRecords;
    }

    /**
     * Return the record whose value matches the specified {@code value} parameter for the
     * specified key.  e.g. if searching for a record having sample ID "SM-1234":
     * <p/>
     * <pre>
     *     getRecordWithMatchingValueForKey(Key.SAMPLE_ID, "SM-1234");
     * </pre>
     *
     * @return matching ManifestRecord or null if none match.
     */
    public ManifestRecord getRecordWithMatchingValueForKey(Metadata.Key key, String value) {
        for (ManifestRecord record : records) {
            if (StringUtils.equals(record.getValueByKey(key), value)) {
                return record;
            }
        }
        return null;
    }

    /**
     * Encapsulates the logic required to mark a session completed:
     * <ul>
     * <li>Mark all scanned, un-quarantined, records as Accessioned</li>
     * <li>Create ManifestEvents for all un-scanned ,un-quarantined, records</li>
     * <li>Set the status of the session to Completed</li>
     * </ul>
     */
    public void completeSession() {

        for (ManifestRecord record : getNonQuarantinedRecords()) {
            if (record.getStatus() != ManifestRecord.Status.SCANNED) {

                String sampleId = record.getValueByKey(Metadata.Key.SAMPLE_ID);
                String message = ManifestRecord.ErrorStatus.MISSING_SAMPLE.formatMessage(Metadata.Key.SAMPLE_ID,
                        sampleId);

                ManifestEvent manifestEvent = new ManifestEvent(ManifestRecord.ErrorStatus.MISSING_SAMPLE,
                        message, record);
                addManifestEvent(manifestEvent);
            } else {
                record.setStatus(ManifestRecord.Status.ACCESSIONED);
            }
        }
        setStatus(SessionStatus.COMPLETED);
    }

    /**
     * Within the context of trying to transfer a source sample, find the corresponding record contained within the
     * session.  Given this context, the method should inform the caller of any issues with attempting to transfer
     * from this record such as the record is quarantined, or not in the correct state within the session, or just
     * not found
     *
     *
     * @param keyForRecord
     * @param sourceForTransfer Sample ID for which the caller wishes to find the corresponding record
     *
     * @return The record that matches the source sample
     */
    public ManifestRecord findRecordForTransferByKey(Metadata.Key keyForRecord, @Nonnull String sourceForTransfer) {

        if (StringUtils.isBlank(sourceForTransfer)) {
            throw new TubeTransferException("A " + keyForRecord.getDisplayName() +
                                            " is required for the transfer to a lab vessel");
        }

        ManifestRecord recordForTransfer = findRecordByKey(sourceForTransfer, keyForRecord);

        if (recordForTransfer.isQuarantined()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE,
                    keyForRecord, sourceForTransfer);
        }

        if (ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE == recordForTransfer.getStatus()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.SOURCE_ALREADY_TRANSFERRED,
                    keyForRecord, sourceForTransfer);
        }

        if (ManifestRecord.Status.ACCESSIONED != recordForTransfer.getStatus()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_READY_FOR_TUBE_TRANSFER,
                    keyForRecord, sourceForTransfer);
        }

        return recordForTransfer;
    }

    /**
     * Encapsulates the logic required to informatically execute a transfer from the collaborator provided tube to
     * a broad tube.  This is primarily done by marking a record within this session as having been transferred.
     * @param sourceCollaboratorSample Sample ID for the source sample.  This should correspond to a record within
*                                 the session
     * @param targetSample             Mercury Sample to which the transfer will be associated
     * @param targetVessel             Lab Vessel to which the transfer will be associated
     * @param user                     Represents the user attempting to make the transfer
     * @param disambiguator            LabEvent disambiguator to avoid unique constraint errors when called in a tight loop
     */
    public void performTransfer(String sourceCollaboratorSample, MercurySample targetSample, LabVessel targetVessel,
                                BspUser user, long disambiguator) {

        ManifestRecord sourceRecord ;

        if(sourceCollaboratorSample != null) {
            sourceRecord = findRecordForTransferByKey(Metadata.Key.SAMPLE_ID, sourceCollaboratorSample);
        } else {
            sourceRecord = findRecordForTransferByKey(Metadata.Key.BROAD_SAMPLE_ID, targetSample.getSampleKey());
        }

        Set<Metadata> metadataToTransfer = new HashSet<>(sourceRecord.getMetadata());
        targetSample.addMetadata(metadataToTransfer);

        sourceRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);

        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), LabEvent.UI_EVENT_LOCATION,
                        disambiguator, user.getUserId(), LabEvent.UI_PROGRAM_NAME);
        targetVessel.addInPlaceEvent(collaboratorTransferEvent);
    }

    /**
     * Scan the specified sample as part of the accessioning process.
     *
     * @param recordReferenceValue The collaborator sample ID the current sample
     * @param recordSampleKey
     */
    public void accessionScan(String recordReferenceValue, Metadata.Key recordSampleKey) {
        ManifestRecord manifestRecord = getRecordWithMatchingValueForKey(recordSampleKey, recordReferenceValue);

        if (manifestRecord == null) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.formatMessage(
                            recordSampleKey, recordReferenceValue));
        }
        manifestRecord.accessionScan(recordSampleKey, recordReferenceValue);
    }

    public boolean canSessionExcludeReceiptTicket() {

        boolean result = true;

        if(StringUtils.isBlank(getReceiptTicket())) {
            result = false;
        }
        return result;
    }


    /**
     * Indicator to denote the availability (complete or otherwise) of a manifest session for the sample registration
     * process.
     */
    public enum SessionStatus implements Displayable {
        OPEN("Manifest Uploaded"), PENDING_SAMPLE_INFO("Awaiting manifest"),
        ACCESSIONING("Accessioning samples"), COMPLETED("Accessioning completed");

        private final String displayName;
        SessionStatus(String displayName) {
            this.displayName = displayName;
        }


        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public UpdateData getUpdateData() {
        return updateData;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("manifestSessionId", manifestSessionId)
                .append("sessionPrefix", sessionPrefix)
                .append("status", status)
                .toString();
    }
}
