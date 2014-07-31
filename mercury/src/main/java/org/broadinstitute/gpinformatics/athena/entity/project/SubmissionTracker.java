package org.broadinstitute.gpinformatics.athena.entity.project;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Entity
@Audited
@Table(name = "SUBMISSION_TRACKER", schema = "athena")
public class SubmissionTracker {

    public static final String MERCURY_SUBMISSION_ID_PREFIX = "MERCURY_SUB_";
    @Id
    @SequenceGenerator(name = "SEQ_SUBMISSION_TRACKER", schema = "athena", sequenceName = "SEQ_SUBMISSION_TRACKER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SUBMISSION_TRACKER")
    @Column(name = "SUBMISSION_TRACKER_ID")
    private Long submissionTrackerId;

    @Column(name = "ACCESSION_IDENTIFIER")
    private String accessionIdentifier;

    @Column(name = "FILE_NAME")
    private String fileName;

    @Column(name = "VERSION")
    private String version;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "RESEARCH_PROJECT_ID")
    private ResearchProject researchProject;

    protected SubmissionTracker() {
    }

    public SubmissionTracker(String testAccessionID, String testFileName, String testVersion) {

        this.accessionIdentifier = testAccessionID;
        this.fileName = testFileName;
        this.version = testVersion;
    }

    public String getAccessionIdentifier() {
        return accessionIdentifier;
    }

    public String getFileName() {
        return fileName;
    }

    public String getVersion() {
        return version;
    }

    public Long getSubmissionTrackerId() {
        return submissionTrackerId;
    }

    protected void setSubmissionTrackerId(Long id) {
        this.submissionTrackerId = id;
    }


    public String getSubmissionIdentifier() {
        return (submissionTrackerId != null) ? (MERCURY_SUBMISSION_ID_PREFIX + submissionTrackerId) : null;
    }

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(ResearchProject researchProject) {
        this.researchProject = researchProject;
    }
}
