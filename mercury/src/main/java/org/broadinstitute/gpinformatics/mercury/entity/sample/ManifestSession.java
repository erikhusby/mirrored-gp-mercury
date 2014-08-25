package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import sun.awt.X11.XBaseWindow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class ManifestSession {
    private final ResearchProject researchProject;
    private final String sessionPrefix;
    private final Long createdBy;
    private Long sessionId;
    private SessionStatus status = SessionStatus.OPEN;
    private Long modifiedBy;
    private List<ManifestRecord> records = new ArrayList<>();
    private Date created;
    private Date modified;

    public ManifestSession(ResearchProject researchProject, String sessionPrefix, BspUser createdBy) {
        this.researchProject = researchProject;
        this.sessionPrefix = sessionPrefix;
        this.createdBy = createdBy.getUserId();
        this.modifiedBy = createdBy.getUserId();
        created = new Date();
        modified = new Date();
    }


    public ResearchProject getResearchProject() {
        return researchProject;
    }

    protected Long getSessionId() {
        return sessionId;
    }

    public String createSessionName() {
        return getSessionPrefix() + getSessionId();
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

    public void addRecord(ManifestRecord testRecord) {

        records.add(testRecord);

    }

    public List<ManifestRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ManifestRecord> records) {
        this.records = records;
    }

    public enum SessionStatus {OPEN}
}
