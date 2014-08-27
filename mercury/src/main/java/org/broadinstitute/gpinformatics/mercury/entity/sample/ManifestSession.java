package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import java.util.List;
import java.util.Map;

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

    public boolean isManifestValid() {

        Multimap<String, ManifestRecord> recordsBySamples = Multimaps.index(records,
                new Function<ManifestRecord, String>() {
                    @Override
                    public String apply(ManifestRecord manifestRecord) {
                        return manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue();
                    }
                });

        Iterable<Map.Entry<String, Collection<ManifestRecord>>> filteredDupes = Iterables.filter(
                recordsBySamples.asMap().entrySet(), new Predicate<Map.Entry<String, Collection<ManifestRecord>>>() {
            @Override
            public boolean apply(Map.Entry<String, Collection<ManifestRecord>> entry) {
                return entry.getValue().size() > 1;
            }
        });

        ArrayList<Map.Entry<String, Collection<ManifestRecord>>> filterEntries = Lists.newArrayList(filteredDupes);

        for (Map.Entry<String, Collection<ManifestRecord>> dupeToProcess : filterEntries) {
            for (ManifestRecord dupeRecord : dupeToProcess.getValue()) {
                dupeRecord.setErrorStatus(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);
                addLogEntry(new ManifestEvent(String.format("For sample %s: %s", dupeToProcess.getKey(),
                        ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getMessage()), dupeRecord,
                        ManifestEvent.Type.ERROR));
            }
        }

        return filterEntries.isEmpty();
    }

    /**
     * Indicator to denote the availability (complete or otherwise) of a manifest session for the sample registration
     * process
     */
    public enum SessionStatus {
        OPEN, COMPLETED
    }
}
