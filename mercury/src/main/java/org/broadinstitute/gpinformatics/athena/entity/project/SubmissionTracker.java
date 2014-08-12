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
import java.beans.Transient;

/**
 * Represents the association between a submitted sample and its' submission identifier.  This will aid the system
 * in tracking and retrieving the status for a pending submission
 */
@Entity
@Audited
@Table(name = "SUBMISSION_TRACKER", schema = "athena")
public class SubmissionTracker {

    public static final String MERCURY_SUBMISSION_ID_PREFIX = "MERCURY_SUB_";

    /**
     * Unique Database identifier for the entity.  This will be used to create the Submissions Identifier
     */
    @Id
    @SequenceGenerator(name = "SEQ_SUBMISSION_TRACKER", schema = "athena", sequenceName = "SEQ_SUBMISSION_TRACKER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SUBMISSION_TRACKER")
    @Column(name = "SUBMISSION_TRACKER_ID")
    private Long submissionTrackerId;

    /**
     * Represents the name of the sample that has been submitted.  This field name may be changed based on the outcome
     * of discussions on what data to specifically send.
     */
    @Column(name = "SUBMITTED_SAMPLE_NAME")
    private String submittedSampleName;

    /**
     * File name and path for the data file being submitted
     */
    @Column(name = "FILE_NAME")
    private String fileName;

    /**
     * version of the data file created
     */
    @Column(name = "VERSION")
    private String version;

    /**
     * research project under which the submission has been made.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "RESEARCH_PROJECT_ID")
    private ResearchProject researchProject;

    protected SubmissionTracker() {
    }

    SubmissionTracker(Long submissionTrackerId, String submittedSampleName, String fileName, String version) {
        this.submissionTrackerId = submissionTrackerId;
        this.submittedSampleName = submittedSampleName;
        this.fileName = fileName;
        this.version = version;
    }

    public SubmissionTracker(String submittedSampleName, String fileName, String version) {
       this(null, submittedSampleName, fileName, version);
    }

    /**
     * This method creates a unique string to be associated with the submission request for the sample file and version
     * that is represented in an instance of the SubmissionTracker.  This identifier will be used to query for status
     * on the progress of the submission
     * @return  A string that is used to identify the submission request
     */
    public String createSubmissionIdentifier() {
        return (submissionTrackerId != null) ? (MERCURY_SUBMISSION_ID_PREFIX + submissionTrackerId) : null;
    }



    public String getSubmittedSampleName() {
        return submittedSampleName;
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

    public ResearchProject getResearchProject() {
        return researchProject;
    }

    public void setResearchProject(ResearchProject researchProject) {
        this.researchProject = researchProject;
    }

    // todo: should be in interface?
    @Transient
    public SubmissionTuple getTuple() {
        return new SubmissionTuple(submittedSampleName, fileName, version);
    }
}
