package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Updatable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.UpdatedEntityInterceptor;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.envers.AuditJoinTable;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manifest session contains the information related to a single manifest upload during the sample accessioning process.
 */
@Entity
@EntityListeners(UpdatedEntityInterceptor.class)
@Audited
@Table(schema = "mercury", name = "MANIFEST_SESSION")
public class ManifestSession implements Updatable {

    public static final String VESSEL_LABEL = "Vessel barcode";

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

    /**
     * For JPA.
     */
    protected ManifestSession() {
    }

    public ManifestSession(ResearchProject researchProject, String pathToManifestFile, BspUser createdBy) {
        this.researchProject = researchProject;
        researchProject.addManifestSession(this);
        sessionPrefix = FilenameUtils.getBaseName(pathToManifestFile);
        getUpdateData().setCreatedBy(createdBy.getUserId());
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

    /**
     * Called during manifest upload, this executes validations against the records in this manifest (taking into
     * context records from other manifests in the same Research Project) that have not already been validated.
     */
    public void validateManifest() {
        List<ManifestRecord> allManifestRecordsAcrossThisResearchProject =
                researchProject.collectNonQuarantinedManifestRecords();

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

                    String message =
                            ManifestRecord.ErrorStatus.MISMATCHED_GENDER
                                    .formatMessage(Metadata.Key.PATIENT_ID, entry.getKey());

                    String otherSessionsWithSamePatientId =
                            describeOtherManifestSessionsWithMatchingRecords(record, entry.getValue());

                    addManifestEvent(new ManifestEvent(ManifestRecord.ErrorStatus.MISMATCHED_GENDER,
                            message + "  " + otherSessionsWithSamePatientId, record));
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
        Multimap<String, ManifestRecord> recordsBySampleId = buildMultimapByKey(
                manifestRecords, Metadata.Key.SAMPLE_ID);

        // Remove entries in this map which are not duplicates (i.e., leave only the duplicates).
        Iterable<Map.Entry<String, Collection<ManifestRecord>>> filteredDuplicateSamples =
                filterForKeysWithMultipleValues(recordsBySampleId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : filteredDuplicateSamples) {
            for (ManifestRecord duplicatedRecord : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (this.equals(duplicatedRecord.getManifestSession())) {
                    String message =
                            ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(Metadata.Key.SAMPLE_ID,
                                    entry.getKey());
                    String sessionsWithDuplicates =
                            describeOtherManifestSessionsWithMatchingRecords(duplicatedRecord, entry.getValue());
                    addManifestEvent(
                            new ManifestEvent(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                                    message + "  " + sessionsWithDuplicates, duplicatedRecord));
                }
            }
        }
    }

    /**
     * Create a presentable description of where the duplicates of {@code thisDuplicate} can be found.
     */
    private String describeOtherManifestSessionsWithMatchingRecords(final ManifestRecord thisRecord,
                                                                    Collection<ManifestRecord> allRecords) {

        // Filter 'thisRecord' from consideration as a duplicate of itself.
        Iterable<ManifestRecord> allButThisRecord = Iterables.filter(allRecords, new Predicate<ManifestRecord>() {
            @Override
            public boolean apply(@Nullable ManifestRecord record) {
                return record != thisRecord;
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
        for (Map.Entry<String, Collection<ManifestRecord>> entry : recordsBySessionName.asMap().entrySet()) {

            StringBuilder messageBuilder = new StringBuilder();

            int numInstances = entry.getValue().size();
            messageBuilder.append(numInstances).append(" ");
            messageBuilder.append(Noun.pluralOf("instance", numInstances));
            messageBuilder.append(" found in ");

            String sessionName = entry.getKey();
            String thisSessionName = thisRecord.getManifestSession().getSessionName();
            messageBuilder.append(
                    sessionName.equals(thisSessionName) ? "this manifest session" : "manifest session " + sessionName);
            messages.add(messageBuilder.toString());
        }
        return StringUtils.join(messages, ", ") + ".";
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
     * Method to find the manifest record that has the specified collaborator barcode.
     */
    public ManifestRecord findRecordByCollaboratorId(String collaboratorBarcode)
            throws TubeTransferException {
        for (ManifestRecord record : records) {
            if (record.getValueByKey(Metadata.Key.SAMPLE_ID).equals(collaboratorBarcode)) {
                return record;
            }
        }
        throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST, Metadata.Key.SAMPLE_ID,
                collaboratorBarcode);
    }

    /**
     * Method to support the user accepting an uploaded manifest in order to continue with the accessioning process.
     * To do so, it will set all non quarantined records within this session to the status of UPLOAD_ACCEPTED
     */
    public void acceptUpload() {
        for (ManifestRecord record : getNonQuarantinedRecords()) {
            record.setStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);
        }
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
        for (ManifestEvent manifestEvent : getManifestEvents()) {
            manifestMessages.add(manifestEvent.getMessage());
        }

        ManifestStatus sessionStatus = new ManifestStatus(getRecords().size(), nonQuarantinedRecords.size(),
                getRecordsByStatus(ManifestRecord.Status.SCANNED).size(), manifestMessages);

        for (ManifestRecord manifestRecord : nonQuarantinedRecords) {
            ManifestRecord.Status manifestRecordStatus = manifestRecord.getStatus();

            if (manifestRecordStatus != ManifestRecord.Status.SCANNED) {
                sessionStatus.addError(ManifestRecord.ErrorStatus.MISSING_SAMPLE
                        .formatMessage(Metadata.Key.SAMPLE_ID, manifestRecord.getSampleId()));
            }
        }
        return sessionStatus;
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
            if (record.getValueByKey(key).equals(value)) {
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
     * @param sourceForTransfer Sample ID for which the caller wishes to find the corresponding record
     *
     * @return The record that matches the source sample
     */
    public ManifestRecord findRecordForTransfer(String sourceForTransfer) {

        if(StringUtils.isBlank(sourceForTransfer)) {
            throw new TubeTransferException("An identifier for the collaborator sample is required for " +
                                            "requiring the transfer to a lab vessel");
        }

        ManifestRecord recordForTransfer = findRecordByCollaboratorId(sourceForTransfer);

        if (recordForTransfer.isQuarantined()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE,
                    Metadata.Key.SAMPLE_ID, sourceForTransfer);
        }

        if (recordForTransfer.getStatus().ordinal() > ManifestRecord.Status.ACCESSIONED.ordinal()) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.SOURCE_ALREADY_TRANSFERRED,
                    Metadata.Key.SAMPLE_ID, sourceForTransfer);
        }

        if (recordForTransfer.getStatus() != ManifestRecord.Status.ACCESSIONED) {
            throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_READY_FOR_TUBE_TRANSFER,
                    Metadata.Key.SAMPLE_ID, sourceForTransfer);
        }

        return recordForTransfer;
    }

    /**
     * Encapsulates the logic required to informatically execute a transfer from the collaborator provided tube to
     * a broad tube.  This is primarily done by marking a record within this session as having been transferred.
     *
     * @param sourceCollaboratorSample Sample ID for the source sample.  This should correspond to a record within
     *                                 the session
     * @param targetSample             Mercury Sample to which the transfer will be associated
     * @param targetVessel             Lab Vessel to which the transfer will be associated
     * @param user                     Represents the user attempting to make the transfer
     */
    public void performTransfer(String sourceCollaboratorSample, MercurySample targetSample, LabVessel targetVessel,
                                BspUser user) {

        ManifestRecord sourceRecord = findRecordForTransfer(sourceCollaboratorSample);

        targetSample.addMetadata(sourceRecord.getMetadata());
        sourceRecord.setStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);

        LabEvent collaboratorTransferEvent =
                new LabEvent(LabEventType.COLLABORATOR_TRANSFER, new Date(), LabEvent.UI_EVENT_LOCATION,
                        LabEvent.DEFAULT_DISAMBIGUATOR, user.getUserId(), LabEvent.UI_PROGRAM_NAME);
        targetVessel.addInPlaceEvent(collaboratorTransferEvent);
    }

    /**
     * Scan the specified sample as part of the accessioning process.
     *
     * @param collaboratorSampleId The collaborator sample ID the current sample
     */
    public void accessionScan(String collaboratorSampleId) {
        ManifestRecord manifestRecord = getRecordWithMatchingValueForKey(Metadata.Key.SAMPLE_ID, collaboratorSampleId);

        if (manifestRecord == null) {
            throw new InformaticsServiceException(
                    ManifestRecord.ErrorStatus.NOT_IN_MANIFEST.formatMessage(
                            Metadata.Key.SAMPLE_ID, collaboratorSampleId));
        }
        manifestRecord.accessionScan();
    }

    /**
     * Indicator to denote the availability (complete or otherwise) of a manifest session for the sample registration
     * process.
     */
    public enum SessionStatus {
        OPEN, COMPLETED
    }

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
