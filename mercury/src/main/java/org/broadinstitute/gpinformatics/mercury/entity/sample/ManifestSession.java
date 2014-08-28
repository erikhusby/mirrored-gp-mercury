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
     *
     * If there is an error with any record in this manifest session, report that some errors exist
     *
     * @return
     */
    public boolean areThereErrors() {

        boolean validationResult = false;

        for (ManifestRecord testRecord : records) {
            validationResult = (testRecord.getErrorStatus() != null);
            if (!validationResult) {
                break;
            }
        }

        return validationResult;
    }

    /**
     * Execute validations against the records in this manifest that have not already been validated
     */
    public void validateManifest() {

        List<ManifestRecord> allManifestRecordsAcrossThisResearchProject =
                collectAllManifestRecordsAcrossThisResearchProject();

        validateDuplicateCollaboratorSampleIDs(allManifestRecordsAcrossThisResearchProject);
        validateInconsistentGenders(allManifestRecordsAcrossThisResearchProject);
    }

    private void validateInconsistentGenders(Collection<ManifestRecord> allManifestRecordsAcrossThisResearchProject) {

        Multimap<String, ManifestRecord> recordsByPatientId = buildMultimapByKey(
                allManifestRecordsAcrossThisResearchProject, Metadata.Key.PATIENT_ID);

        Iterable<Map.Entry<String, Collection<ManifestRecord>>> patientsWithInconsistentGenders =
                filterForPatientsWithInconsistentGenders(recordsByPatientId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : patientsWithInconsistentGenders) {
            for (ManifestRecord duplicatedRecord : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (duplicatedRecord.getSession().equals(this)) {
                    duplicatedRecord.setErrorStatus(ManifestRecord.ErrorStatus.MISMATCHED_GENDER);

                    String message =
                            ManifestRecord.ErrorStatus.MISMATCHED_GENDER.formatMessage("patient ID", entry.getKey());
                    addLogEntry(new ManifestEvent(message, duplicatedRecord, ManifestEvent.Type.ERROR));
                }
            }
        }

    }

    private Iterable<Map.Entry<String, Collection<ManifestRecord>>> filterForPatientsWithInconsistentGenders(
            Multimap<String, ManifestRecord> recordsByPatientId) {

        return Iterables.filter(recordsByPatientId.asMap().entrySet(), new Predicate<Map.Entry<String, Collection<ManifestRecord>>>() {

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

    private void validateDuplicateCollaboratorSampleIDs(
            Collection<ManifestRecord> allManifestRecordsAcrossThisResearchProject) {

        Multimap<String, ManifestRecord> recordsBySampleId = buildMultimapByKey(
                allManifestRecordsAcrossThisResearchProject, Metadata.Key.SAMPLE_ID);

        Iterable<Map.Entry<String, Collection<ManifestRecord>>> filteredDuplicateSamples =
                filterForKeysWithMultipleValues(recordsBySampleId);

        for (Map.Entry<String, Collection<ManifestRecord>> entry : filteredDuplicateSamples) {
            for (ManifestRecord duplicatedRecord : entry.getValue()) {
                // Ignore ManifestSessions that are not this ManifestSession, they will not have errors added by
                // this logic.
                if (duplicatedRecord.getSession().equals(this)) {
                    duplicatedRecord.setErrorStatus(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);

                    String message =
                            ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage("sample ID", entry.getKey());
                    addLogEntry(new ManifestEvent(message, duplicatedRecord, ManifestEvent.Type.ERROR));
                }
            }
        }
    }

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

    private Multimap<String, ManifestRecord> buildMultimapByKey(
            Collection<ManifestRecord> allRecordsAcrossThisResearchProject, final Metadata.Key key) {
        return Multimaps.index(allRecordsAcrossThisResearchProject,
                new Function<ManifestRecord, String>() {
                    @Override
                    public String apply(ManifestRecord manifestRecord) {
                        return manifestRecord.getMetadataByKey(key).getValue();
                    }
                });
    }

    private Set<String> collectAllValuesForKeyInThisManifest(Metadata.Key targetKey) {

        Set<String> allSampleIDs = new HashSet<>();

        for (ManifestRecord record : records) {
            allSampleIDs.add(record.getMetadataByKey(targetKey).getValue());
        }


        return allSampleIDs;
    }

    private List<ManifestRecord> collectAllManifestRecordsAcrossThisResearchProject() {
        List<ManifestRecord> allRecords = new ArrayList<>();

        for (ManifestSession manifestSession : researchProject.getManifestSessions()) {
            allRecords.addAll(manifestSession.getRecords());
        }

        return allRecords;
    }

    /**
     * Indicator to denote the availability (complete or otherwise) of a manifest session for the sample registration
     * process
     */
    public enum SessionStatus {
        OPEN, COMPLETED
    }
}
