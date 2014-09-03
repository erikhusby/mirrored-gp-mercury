package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
 * Manifest session contains the information related to a single manifest upload during the sample registration process.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "MANIFEST_SESSION")
public class ManifestSession {

    private  static final String SAMPLE_ID_KEY = "Sample ID";

    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_SESSION", schema = "mercury", sequenceName = "SEQ_MANIFEST_SESSION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_SESSION")
    Long manifestSessionId;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "RESEARCH_PROJECT_ID")
    private ResearchProject researchProject;

    @Column(name = "SESSION_PREFIX")
    private String sessionPrefix;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.OPEN;

    @Column(name = "MODIFIED_BY")
    private Long modifiedBy;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "manifestSession")
    private List<ManifestRecord> records = new ArrayList<>();

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "session")
    private List<ManifestEvent> manifestEvents = new ArrayList<>();

    /**
     * For JPA
     */
    protected ManifestSession() {
    }

    public ManifestSession(ResearchProject researchProject, String sessionPrefix, BspUser createdBy) {
        this.researchProject = researchProject;
        this.researchProject.addManifestSession(this);

        this.sessionPrefix = sessionPrefix;
        this.createdBy = createdBy.getUserId();
        this.modifiedBy = createdBy.getUserId();
        createdDate = new Date();
        modifiedDate = new Date();
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public Long getManifestSessionId() {
        return manifestSessionId;
    }

    public String getSessionName() {
        return getSessionPrefix() + getManifestSessionId();
    }

    protected String getSessionPrefix() {
        return sessionPrefix;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(BspUser modifiedBy) {
        this.modifiedBy = modifiedBy.getUserId();
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
        manifestEvent.setSession(this);
    }

    public List<ManifestEvent> getManifestEvents() {
        return manifestEvents;
    }

    /**
     * Called during manifest upload, this executes validations against the records in this manifest that have
     * not already been validated.
     */
    public void validateManifest() {

        List<ManifestRecord> allManifestRecordsAcrossThisResearchProject =
                collectAllManifestRecordsAcrossThisResearchProject();

        validateDuplicateCollaboratorSampleIDs(allManifestRecordsAcrossThisResearchProject);
        validateInconsistentGenders(allManifestRecordsAcrossThisResearchProject);
    }

    /**
     * Encapsulates the logic to validate a given set of manifest records for inconsistent gender values for a
     * patient ID.
     *
     * @param manifestRecordsEligibleForValidation Collection of all manifest records against which to perform
     *                                             gender mismatch validation
     */
    private void validateInconsistentGenders(Collection<ManifestRecord> manifestRecordsEligibleForValidation) {

        Multimap<String, ManifestRecord> recordsByPatientId = buildMultimapByKey(
                manifestRecordsEligibleForValidation, Metadata.Key.PATIENT_ID);

        Iterable<Map.Entry<String, Collection<ManifestRecord>>> patientsWithInconsistentGenders =
                filterForPatientsWithInconsistentGenders(recordsByPatientId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : patientsWithInconsistentGenders) {
            for (ManifestRecord duplicatedRecord : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (this.equals(duplicatedRecord.getManifestSession())) {

                    String message =
                            ManifestRecord.ErrorStatus.MISMATCHED_GENDER.formatMessage("patient ID", entry.getKey());
                    addManifestEvent(new ManifestEvent(ManifestEvent.Severity.ERROR, message, duplicatedRecord));
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
                            allGenders.add(manifestRecord.getMetadataByKey(Metadata.Key.GENDER).getValue());
                        }
                        return allGenders.size() > 1;
                    }
                });
    }

    /**
     * Encapsulates the logic to validate a given set of manifest records for non-unique references to collaborator
     * sample ID.
     *
     * @param allEligibleManifestRecords Collection of all manifest records against which to perform unique sample id
     *                                   validation
     */
    private void validateDuplicateCollaboratorSampleIDs(Collection<ManifestRecord> allEligibleManifestRecords) {

        Multimap<String, ManifestRecord> recordsBySampleId = buildMultimapByKey(
                allEligibleManifestRecords, Metadata.Key.SAMPLE_ID);

        Iterable<Map.Entry<String, Collection<ManifestRecord>>> filteredDuplicateSamples =
                filterForKeysWithMultipleValues(recordsBySampleId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : filteredDuplicateSamples) {
            for (ManifestRecord duplicatedRecord : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (duplicatedRecord.getManifestSession().equals(this)) {
                    String message =
                            ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage(SAMPLE_ID_KEY, entry.getKey());
                    addManifestEvent(new ManifestEvent(ManifestEvent.Severity.QUARANTINED, message, duplicatedRecord));
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
                        return manifestRecord.getMetadataByKey(key).getValue();
                    }
                });
    }

    /**
     * Helper method to extract all manifest records eligible for validation.  Records for which validation has been
     * run and has found errors are not eligible for validation.
     *
     * @return A list of all Manifest record
     */
    private List<ManifestRecord> collectAllManifestRecordsAcrossThisResearchProject() {
        List<ManifestRecord> allRecords = new ArrayList<>();

        for (ManifestSession manifestSession : researchProject.getManifestSessions()) {
            allRecords.addAll(manifestSession.getNonQuarantinedRecords());
        }

        return allRecords;
    }

    private List<ManifestRecord> getNonQuarantinedRecords() {
        List<ManifestRecord> allRecords = new ArrayList<>();
        for (ManifestRecord manifestRecord : getRecords()) {
            if (!manifestRecord.quarantinedErrorExists()) {
                allRecords.add(manifestRecord);
            }
        }
        return allRecords;
    }

    /**
     * Called before the manifest session is set to completed, this will ensure that all records on the session are
     * in the proper state.  If not, manifest events are added to the record to indicate that it has not
     * been scanned.
     */
    public void validateForClose() {
        // Confirm all records have scanned status.
        for (ManifestRecord record : records) {
            if ((record.getStatus() != ManifestRecord.Status.SCANNED) && !record.quarantinedErrorExists()) {

                String sampleId = record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue();
                String message = ManifestRecord.ErrorStatus.MISSING_SAMPLE.formatMessage(SAMPLE_ID_KEY, sampleId);

                ManifestEvent manifestEvent = new ManifestEvent(ManifestEvent.Severity.ERROR, message, record);
                manifestEvents.add(manifestEvent);
            }
        }
    }

    /**
     * hasErrors is used to determine if any manifest event entries exist that can be considered errors
     *
     * @return true if even one manifest event entry can be considered an error
     */
    public boolean hasErrors() {
        return hasManifestEventOfType(EnumSet.of(ManifestEvent.Severity.ERROR, ManifestEvent.Severity.QUARANTINED));
    }

    /**
     * Helper method to check if there are any manifest event entries of a given type
     *
     * @param severities set of types to check for entries against
     *
     * @return true if even one event entry matches a type in the given set of event types
     */
    private boolean hasManifestEventOfType(Set<ManifestEvent.Severity> severities) {
        for (ManifestEvent manifestEvent : manifestEvents) {
            if (severities.contains(manifestEvent.getSeverity())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides the caller with the ability to find a manifest record on the current manifest session that corresponds
     * to the given sample id
     *
     * @param collaboratorBarcode Collaborator sample ID for which this method intends to find a manifest record
     *
     * @return the found record (if it exists)
     */
    public ManifestRecord findScannedRecord(String collaboratorBarcode) {
        for (ManifestRecord record : records) {
            if (record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(collaboratorBarcode)) {
                if (record.getStatus() != ManifestRecord.Status.SCANNED) {
                    throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_READY_FOR_ACCESSIONING,
                            SAMPLE_ID_KEY,
                            collaboratorBarcode);
                }
                return record;
            }
        }
        throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST, SAMPLE_ID_KEY, collaboratorBarcode);
    }

    /**
     * Encapsulates the series of steps to perform when a user wishes to scan a source sample tube
     *
     * @param sampleId collaborator sample ID of the tube being scanned
     *
     * @return Manifest record associated with the scanned
     */
    public ManifestRecord scanSample(String sampleId) {

        ManifestRecord foundRecord = null;
        try {
            foundRecord = findRecordByState(sampleId);
        } catch (TubeTransferException e) {
            addManifestEvent(new ManifestEvent(ManifestEvent.Severity.ERROR,
                    e.getErrorStatus().formatMessage(SAMPLE_ID_KEY, sampleId)
            ));
            throw e;
        }
        if (foundRecord.quarantinedErrorExists()) {

            Set<String> fatalMessages = foundRecord.getQuarantinedRecordMessages();

            throw new TubeTransferException(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE,
                    SAMPLE_ID_KEY,
                    sampleId,
                    StringUtils.join(fatalMessages, ", "));
        }
        foundRecord.setStatus(ManifestRecord.Status.SCANNED);
        return foundRecord;
    }

    private ManifestRecord findRecordByState(String collaboratorBarcode)
            throws TubeTransferException {
        for (ManifestRecord record : records) {
            if (record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(collaboratorBarcode)) {
                return record;
            }
        }
        throw new TubeTransferException(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST, SAMPLE_ID_KEY, collaboratorBarcode);
    }

    public void acceptUpload() {
        for (ManifestRecord record : getNonQuarantinedRecords()) {
            // record.setStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);
        }
    }

    /**
     * Indicator to denote the availability (complete or otherwise) of a manifest session for the sample registration
     * process.
     */
    public enum SessionStatus {
        OPEN, COMPLETED
    }
}
