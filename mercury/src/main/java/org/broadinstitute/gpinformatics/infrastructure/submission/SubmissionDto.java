/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTracker;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationContam;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionDto implements ISubmissionTuple {
    private static final long serialVersionUID = 8359394045710346776L;

    @JsonIgnore
    NumberFormat numberFormat = NumberFormat.getInstance();
    @JsonIgnore
    public static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);

    @JsonIgnore
    private SubmissionStatusDetailBean statusDetailBean;

    @JsonIgnore
    private Aggregation aggregation;

    @JsonIgnore
    private Double qualityMetric;

    @JsonIgnore
    private Double pctContamination;

    @JsonIgnore
    private Date dateCompleted;

    @JsonIgnore
    private LevelOfDetection levelOfDetection;

    @JsonIgnore
    private String[] submittedErrorsArray;

    @JsonProperty
    private SubmissionTuple submissionTuple;

    @JsonProperty(value = SubmissionDto.SubmissionField.PRODUCT_ORDERS)
    private Set<String> productOrders = new HashSet<>();

    String getSubmissionTupleString() {
        String value = "";
        if (submissionTuple != null) {
            value = submissionTuple.toString();
        }
        return value;
    }

    @JsonProperty(value = SubmissionDto.SubmissionField.UUID)
    private String uuid="";

    @JsonProperty(value = SubmissionDto.SubmissionField.SAMPLE_NAME)
    private String sample="";

    @JsonProperty(value = SubmissionDto.SubmissionField.DATA_TYPE)
    private String datatype="";

    @JsonProperty(value = SubmissionDto.SubmissionField.AGGREGATION_PROJECT)
    private String project="";

    @JsonProperty(value = SubmissionDto.SubmissionField.FILE_TYPE)
    private String fileTypeString = "";

    @JsonIgnore
    private FileType fileType=null;

    @JsonProperty(value = SubmissionDto.SubmissionField.VERSION)
    private Integer version;

    @JsonProperty(value = SubmissionDto.SubmissionField.QUALITY_METRIC)
    private String qualityMetricString="";

    @JsonProperty(value = SubmissionDto.SubmissionField.CONTAMINATION_STRING)
    private String contaminationString="";

    @JsonProperty(value = SubmissionDto.SubmissionField.RESEARCH_PROJECT)
    private String rpid="";

    @JsonProperty(value = SubmissionDto.SubmissionField.LANES_IN_AGGREGATION)
    private Integer readGroupCount;

    @JsonProperty(value = SubmissionDto.SubmissionField.FILE_NAME)
    private String fileName="";

    @JsonProperty(value = SubmissionDto.SubmissionField.PROCESSING_LOCATION)
    private String processingLocation = "";

    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_VERSION)
    private Integer submittedVersion;

    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_ERRORS)
    private List<String> submittedErrors;

    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_STATUS)
    private String submittedStatus="";

    @JsonProperty(value = SubmissionDto.SubmissionField.STATUS_DATE)
    private String statusDate="";

    @JsonProperty(value = SubmissionDto.SubmissionField.BIO_PROJECT)
    private String bioProject="";

    @JsonProperty(value = SubmissionDto.SubmissionField.LIBRARY_DESCRIPTOR)
    private String libraryDescriptor="";

    @JsonProperty(value = SubmissionDto.SubmissionField.SUBMISSION_SITE)
    private String submissionSite="";

    @JsonProperty(value = SubmissionDto.SubmissionField.FINGERPRINT_LOD_MIN)
    private String fingerprintLodMin="";

    @JsonProperty(value = SubmissionDto.SubmissionField.FINGERPRINT_LOD_MAX)
    private String fingerprintLodMax="";

    public SubmissionDto() {
    }

    public SubmissionDto(Aggregation aggregation, @Nullable SubmissionStatusDetailBean statusDetailBean) {
        this.aggregation = aggregation;
        this.statusDetailBean = statusDetailBean;

        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setGroupingUsed(false);

        initializeFieldValues();
    }

    private void initializeFieldValues() {
        dateCompleted = null;
        if (aggregation != null) {
            sample = aggregation.getSample();
            datatype = SubmissionLibraryDescriptor.getNormalizedLibraryName(aggregation.getDataType());
            project = aggregation.getProject();
            version = aggregation.getVersion();
            rpid = aggregation.getMercuryProject();
            processingLocation = aggregation.getProcessingLocation();
            submissionTuple = aggregation.getSubmissionTuple();
            fileType = submissionTuple.getFileType();
            fileTypeString = fileType.toString();
            if (StringUtils.isNotBlank(datatype)) {
                qualityMetric = aggregation.getQualityMetric();
            }
            qualityMetricString = aggregation.getQualityMetricString();
            AggregationContam aggregationContam = aggregation.getAggregationContam();
            if (aggregationContam != null) {
                pctContamination = aggregationContam.getPctContamination();
            }
            levelOfDetection = aggregation.getLevelOfDetection();
            if (levelOfDetection != null) {
                fingerprintLodMin = numberFormat.format(levelOfDetection.getMin());
                fingerprintLodMax = numberFormat.format(levelOfDetection.getMax());
            }
            readGroupCount = aggregation.getReadGroupCount();
            contaminationString = aggregation.getContaminationString();
            productOrders.addAll(aggregation.getPicardAggregationSample().getProductOrderList());
        }
        initializeStatusDetailBean(statusDetailBean);
        submittedErrorsArray = submittedErrors.toArray(new String[submittedErrors.size()]);
    }

    private void initializeStatusDetailBean(SubmissionStatusDetailBean statusDetail) {
        this.statusDetailBean = statusDetail;
        if (statusDetail != null) {
            uuid = statusDetailBean.getUuid();
            submittedVersion=null;
                String submittedVersionString = statusDetail.getSubmittedVersion();
                if (StringUtils.isNotBlank(submittedVersionString)) {
                    submittedVersion = Integer.parseInt(submittedVersionString);
                }
            submittedErrors = statusDetail.getErrors();
            submittedStatus = statusDetail.getStatusString();
            if (statusDetail.getLastStatusUpdate() != null) {
                statusDate = DATE_FORMAT.format(statusDetail.getLastStatusUpdate());
            }
            BioProject project = statusDetail.getBioproject();
            if (project != null) {
                bioProject = String.format("%s %s", project.getAccession(), project.getAlias());
            }
            libraryDescriptor = statusDetail.getSubmissionDatatype();
            submissionSite = statusDetail.getSite();

        } else {
            submittedErrors = Collections.emptyList();
        }
    }

    public String getUuid() {
        return uuid;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    @Override
    public String getSampleName() {
        return sample;
    }

    @Override
    public String getDataType() {
        return datatype;
    }

    public String getAggregationProject() {
        return project;
    }

    @Override
    public FileType getFileType() {
        return fileType;
    }

    @Override
    public String getVersionString(){
        return String.valueOf(version);
    }

    public Integer getVersion() {
        return version;
    }

    Double getQualityMetric() {
        return qualityMetric;
    }

    public String getQualityMetricString() {
        return qualityMetricString;
    }

    String getContaminationString() {
        return contaminationString;
    }

    Double getContamination() {
        return pctContamination;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    LevelOfDetection getFingerprintLOD() {
        return levelOfDetection;
    }

    public String getFingerprintLodMax() {
        return fingerprintLodMax;
    }

    public String getFingerprintLodMin(){
        return fingerprintLodMin;
    }

    public String getResearchProject() {
        return rpid;
    }

    @Override @JsonIgnore
    public String getProject() {
        return rpid;
    }

    int getLanesInAggregation() {
        return readGroupCount;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String getProcessingLocation() {
        return processingLocation;
    }

    public Integer getSubmittedVersion() {
        return submittedVersion;
    }

    public List<String> getSubmittedErrors() {
        return submittedErrors;
    }

    public String[] getSubmittedErrorsArray() {
        return submittedErrorsArray;
    }

    public String getSubmittedStatus() {
        return submittedStatus;
    }

    public String getStatusDate() {
        return statusDate;
    }

    public String getBioProject() {
        return bioProject;
    }

    String getSubmissionLibraryDescriptor() {
        return libraryDescriptor;
    }

    String getSubmissionRepositoryName() {
        return submissionSite;
    }

    @Override
    public SubmissionTuple getSubmissionTuple() {
        return submissionTuple;
    }

    public void setStatusDetailBean(SubmissionStatusDetailBean statusDetailBean) {
        initializeStatusDetailBean(statusDetailBean);
    }

    public SubmissionStatusDetailBean getStatusDetailBean() {
        return statusDetailBean;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public SubmissionTracker buildSubmissionTracker(String datatype) {
        return new SubmissionTracker(project, sample, String.valueOf(version), FileType.BAM, processingLocation,
            SubmissionLibraryDescriptor.getNormalizedLibraryName(datatype));
    }

    public Set<String> getProductOrders() {
        return productOrders;
    }

    public class SubmissionField {
        public static final String SUBMISSION_TUPLE = "submissionTuple";
        public static final String SAMPLE_NAME = "sampleName";
        public static final String DATA_TYPE = "dataType";
        public static final String RESEARCH_PROJECT = "researchProject";
        public static final String AGGREGATION_PROJECT = "aggregationProject";
        public static final String FILE_NAME = "fileName";
        public static final String FILE_TYPE = "fileType";
        public static final String VERSION = "version";
        public static final String QUALITY_METRIC = "qualityMetric";
        public static final String CONTAMINATION_STRING = "contaminationString";
        public static final String FINGERPRINT_LOD_MIN = "fingerprintLodMin";
        public static final String FINGERPRINT_LOD_MAX = "fingerprintLodMax";
        public static final String LANES_IN_AGGREGATION = "lanesInAggregation";
        public static final String SUBMITTED_VERSION = "submittedVersion";
        public static final String SUBMITTED_STATUS = "submittedStatus";
//        public static final String DATE_COMPLETED = "dateCompleted";
        public static final String BIO_PROJECT = "bioProject";
        public static final String PRODUCT_ORDERS = "productOrders";
        public static final String SUBMITTED_ERRORS = "submittedErrors";
        public static final String STATUS_DATE = "statusDate";
        public static final String LIBRARY_DESCRIPTOR = "libraryDescriptor";
        public static final String SUBMISSION_SITE = "site";
        public static final String UUID = "uuid";
        public static final String PROCESSING_LOCATION = "processingLocation";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubmissionDto that = (SubmissionDto) o;

        return new EqualsBuilder()
            .append(numberFormat, that.numberFormat)
            .append(statusDetailBean, that.statusDetailBean)
            .append(aggregation, that.aggregation)
            .append(qualityMetric, that.qualityMetric)
            .append(pctContamination, that.pctContamination)
            .append(dateCompleted, that.dateCompleted)
            .append(levelOfDetection, that.levelOfDetection)
            .append(submittedErrorsArray, that.submittedErrorsArray)
            .append(submissionTuple, that.submissionTuple)
            .append(uuid, that.uuid)
            .append(fileTypeString, that.fileTypeString)
            .append(qualityMetricString, that.qualityMetricString)
            .append(contaminationString, that.contaminationString)
            .append(readGroupCount, that.readGroupCount)
            .append(fileName, that.fileName)
            .append(processingLocation, that.processingLocation)
            .append(submittedVersion, that.submittedVersion)
            .append(submittedErrors, that.submittedErrors)
            .append(submittedStatus, that.submittedStatus)
            .append(statusDate, that.statusDate)
            .append(bioProject, that.bioProject)
            .append(libraryDescriptor, that.libraryDescriptor)
            .append(submissionSite, that.submissionSite)
            .append(fingerprintLodMin, that.fingerprintLodMin)
            .append(fingerprintLodMax, that.fingerprintLodMax)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(numberFormat)
            .append(statusDetailBean)
            .append(aggregation)
            .append(qualityMetric)
            .append(pctContamination)
            .append(dateCompleted)
            .append(levelOfDetection)
            .append(submittedErrorsArray)
            .append(submissionTuple)
            .append(uuid)
            .append(fileTypeString)
            .append(qualityMetricString)
            .append(contaminationString)
            .append(readGroupCount)
            .append(fileName)
            .append(processingLocation)
            .append(submittedVersion)
            .append(submittedErrors)
            .append(submittedStatus)
            .append(statusDate)
            .append(bioProject)
            .append(libraryDescriptor)
            .append(submissionSite)
            .append(fingerprintLodMin)
            .append(fingerprintLodMax)
            .toHashCode();
    }
}
