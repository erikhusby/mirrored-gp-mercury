package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.Date;

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
     * TODO: Remove this field!
     */
    @Column(name = "FILE_NAME")
    private String fileName;

    /**
     * File type of the file being submitted
     */
    @Enumerated(EnumType.STRING)
    private BassDTO.FileType fileType;

    @Enumerated(EnumType.STRING)
    private BassFileType fileType;
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

    private Date requestDate;

    protected SubmissionTracker() {
    }

    SubmissionTracker(Long submissionTrackerId, String submittedSampleName, BassDTO.FileType fileType, String version) {
    SubmissionTracker(Long submissionTrackerId, String submittedSampleName, BassFileType fileType, String version) {
        this.submissionTrackerId = submissionTrackerId;
        this.submittedSampleName = submittedSampleName;
        this.fileType = fileType;
        this.version = version;
        requestDate = new Date();
    }

    public SubmissionTracker(String submittedSampleName, BassFileType fileType, String version) {
       this(null, submittedSampleName, fileType, version);
    }

    /**
     * This method creates a unique string to be associated with the submission request for the sample file and version
     * that is represented in an instance of the SubmissionTracker.  This identifier will be used to query for status
     * on the progress of the submission
     * @return  A string that is used to identify the submission request
     */
    public String createSubmissionIdentifier() {
        String id = null;
        if (submissionTrackerId != null) {
            String date = "";
            /*
             * requestDate didn't exist initially, so it may be null, in which case only the ID should be used for
             * backwards compatibility with the implementation of createSubmissionIdentifier() from before requestDate
             * was added.
             */
            if (requestDate != null) {
                date = new SimpleDateFormat("YYYYMMdd").format(requestDate);
            }
            id = MERCURY_SUBMISSION_ID_PREFIX + date + submissionTrackerId;
        }
        return id;
    }

    public BassFileType getFileType() {
        return fileType;
    }

    public void setFileType(BassFileType fileType) {
        this.fileType = fileType;
    }

    public String getSubmittedSampleName() {
        return submittedSampleName;
    }

    public String getFileName() {
        return fileName;
    }

    @Deprecated
    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public Date getRequestDate() {
        return requestDate;
    }

    // todo: should be in interface?
    @Transient
    public SubmissionTuple getKey() {
        return new SubmissionTuple(submittedSampleName, fileType, version);
    public SubmissionTuple getTuple() {
        return new SubmissionTuple(submittedSampleName, fileType, version);
    }

}
