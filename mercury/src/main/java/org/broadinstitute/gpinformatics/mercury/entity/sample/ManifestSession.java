package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
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

    @Id
    @SequenceGenerator(name = "SEQ_MANIFEST_SESSION", schema = "mercury", sequenceName = "SEQ_MANIFEST_SESSION")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MANIFEST_SESSION")
    private Long manifestSessionId;

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

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "session")
    private List<ManifestRecord> records = new ArrayList<>();

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "session")
    private List<ManifestEvent> logEntries = new ArrayList<>();

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

    protected Long getManifestSessionId() {
        return manifestSessionId;
    }

    public String createSessionName() {
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
        record.setSession(this);
    }

    public List<ManifestRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ManifestRecord> records) {
        this.records = records;
    }

    public void addLogEntry(ManifestEvent logEntry) {

        logEntries.add(logEntry);
        logEntry.setSession(this);
    }

    public List<ManifestEvent> getLogEntries() {
        return logEntries;
    }

    /**
     * If there is an error with any record in this manifest session, report that some errors exist.
     */
    public boolean didSomethingGetLogged() {

        boolean validationResult = false;

        for (ManifestRecord testRecord : records) {
            validationResult = (!testRecord.getLogEntries().isEmpty());
            if (validationResult) {
                break;
            }
        }

        return validationResult;
    }

    /**
     * Execute validations against the records in this manifest that have not already been validated.
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
                if (duplicatedRecord.getSession().equals(this)) {

                    String message =
                            ManifestRecord.ErrorStatus.MISMATCHED_GENDER.formatMessage("patient ID", entry.getKey());
                    addLogEntry(new ManifestEvent(message, ManifestEvent.Type.ERROR, duplicatedRecord));
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
                if (duplicatedRecord.getSession().equals(this)) {
                    String message =
                            ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage("sample ID", entry.getKey());
                    addLogEntry(new ManifestEvent(message, ManifestEvent.Type.FATAL, duplicatedRecord));
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
            for (ManifestRecord manifestRecord : manifestSession.getRecords()) {
                if (!manifestRecord.fatalErrorExists()) {
                    allRecords.add(manifestRecord);
                }
            }
        }

        return allRecords;
    }

    public void close() {
        // Confirm all records have scanned status.
        for (ManifestRecord record : records) {
            if (!(record.getStatus() == ManifestRecord.Status.SCANNED)) {
                String sampleId = record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue();
                String message = ManifestRecord.ErrorStatus.MISSING_SAMPLE.formatMessage("sample ID", sampleId);

                ManifestEvent manifestEvent = new ManifestEvent(message, ManifestEvent.Type.FATAL);
                logEntries.add(manifestEvent);
            }
        }
    }

    public boolean hasErrors() {
        return hasManifestEventOfType(EnumSet.of(ManifestEvent.Type.ERROR, ManifestEvent.Type.FATAL));
    }

    private boolean hasManifestEventOfType(Set<ManifestEvent.Type> types) {
        for (ManifestEvent logEntry : logEntries) {
            if (types.contains(logEntry.getLogType())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Indicator to denote the availability (complete or otherwise) of a manifest session for the sample registration
     * process.
     */
    public enum SessionStatus {
        OPEN, COMPLETED
    }
}
