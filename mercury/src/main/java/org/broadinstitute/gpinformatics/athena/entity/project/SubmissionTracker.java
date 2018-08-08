package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.ISubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionLibraryDescriptor;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
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
import javax.persistence.Transient;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the association between a submitted sample and its' submission identifier.  This will aid the system
 * in tracking and retrieving the status for a pending submission
 */
@Entity
@Audited
@Table(name = "SUBMISSION_TRACKER", schema = "athena")
public class SubmissionTracker implements ISubmissionTuple {

    public static final String MERCURY_SUBMISSION_ID_PREFIX = "MERCURY_SUB_";

    /**
     * Unique Database identifier for the entity.  This will be used to create the Submissions Identifier
     */
    @Id
    @SequenceGenerator(name = "SEQ_SUBMISSION_TRACKER", schema = "athena", sequenceName = "SEQ_SUBMISSION_TRACKER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SUBMISSION_TRACKER")
    @Column(name = "SUBMISSION_TRACKER_ID")
    private Long submissionTrackerId;

    private String project;

    /**
     * Represents the name of the sample that has been submitted.  This field name may be changed based on the outcome
     * of discussions on what data to specifically send.
     */
    @Column(name = "SUBMITTED_SAMPLE_NAME")
    private String submittedSampleName;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    /**
     * version of the data file created
     */
    @Column(name = "VERSION")
    private String version;

    // OnPrem or GCP
    @Column(name = "PROCESSING_LOCATION")
    private String processingLocation;

    @Column(name = "DATA_TYPE")
    private String dataType;

    /**
     * research project under which the submission has been made.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "RESEARCH_PROJECT_ID")
    private ResearchProject researchProject;

    private Date requestDate;

    protected SubmissionTracker() {
    }

    SubmissionTracker(Long submissionTrackerId, String project, String submittedSampleName, String version,
                      FileType fileType, String processingLocation, String dataType) {
        this.submissionTrackerId = submissionTrackerId;
        this.submittedSampleName = submittedSampleName;
        this.project = project;
        this.fileType = fileType;
        this.version = version;
        this.processingLocation = processingLocation;
        this.dataType = SubmissionLibraryDescriptor.getNormalizedLibraryName(dataType);
        this.requestDate = new Date();
    }

    public SubmissionTracker(String project, String submittedSampleName, String version, FileType fileType, String processingLocation, String dataType) {
        this(null, project, submittedSampleName, version, fileType, processingLocation, dataType);
    }

    public static Map<String, SubmissionTracker> uuidMap(List<SubmissionTracker> submissionTrackers) {
        Map<String, SubmissionTracker> uuidMap = new HashMap<>(submissionTrackers.size());
        for (SubmissionTracker submissionTracker : submissionTrackers) {
            uuidMap.put(submissionTracker.createSubmissionIdentifier(), submissionTracker);
        }
        return uuidMap;
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

    public String getSubmittedSampleName() {
        return submittedSampleName;
    }

    @Override
    public String getSampleName() {
        return getSubmittedSampleName();
    }

    @Override
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    @Override
    public FileType getFileType() {
        return fileType;
    }

    @Override
    public String getProcessingLocation() {
        return processingLocation;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String getVersionString() {
        return version;
    }

    public Long getSubmissionTrackerId() {
        return submissionTrackerId;
    }

    void setSubmissionTrackerId(Long id) {
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

    public void setProcessingLocation(String processingLocation) {
        this.processingLocation = processingLocation;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    @Transient
    public SubmissionTuple getSubmissionTuple() {
        return new SubmissionTuple(project, researchProject.getJiraTicketKey(), submittedSampleName, version, processingLocation, dataType);
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || (!OrmUtil.proxySafeIsInstance(o, SubmissionTracker.class))) {
            return false;
        }

        if (!(o instanceof SubmissionTracker)) {
            return false;
        }

        SubmissionTracker that = OrmUtil.proxySafeCast(o, SubmissionTracker.class);

        EqualsBuilder equalsBuilder = new EqualsBuilder()
            .append(getSubmissionTuple(), that.getSubmissionTuple())
            .append(getRequestDate(), that.getRequestDate());
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(17, 37)
            .append(getSubmissionTuple())
            .append(getRequestDate());
        return hashCodeBuilder.toHashCode();
    }
}
