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
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.SubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionDto implements Serializable {
    private static final long serialVersionUID = 8359394045710346776L;

    @JsonIgnore
    NumberFormat numberFormat = NumberFormat.getInstance();
    @JsonIgnore
    public static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);

    @JsonIgnore
    private Collection<ProductOrder> productOrders;

    @JsonIgnore
    private SubmissionStatusDetailBean statusDetailBean;

    @JsonIgnore
    private BassDTO bassDTO;

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
    private String filePath;

    @JsonIgnore
    private String[] submittedErrorsArray;

    @JsonProperty(value = SubmissionDto.SubmissionField.UUID)
    private String uuid="";

    @JsonProperty(value = SubmissionDto.SubmissionField.SAMPLE_NAME)
    private String sample="";

    @JsonProperty(value = SubmissionDto.SubmissionField.DATA_TYPE)
    private String datatype="";

    @JsonProperty(value = SubmissionDto.SubmissionField.PRODUCT_ORDERS)
    private Collection<String> productOrdersString;

    @JsonProperty(value = SubmissionDto.SubmissionField.AGGREGATION_PROJECT)
    private String project="";

    @JsonProperty(value = SubmissionDto.SubmissionField.FILE_TYPE)
    private String fileTypeString="";

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

    public SubmissionDto(@Nonnull BassDTO bassDTO, Aggregation aggregation,
                         @Nonnull Collection<ProductOrder> productOrders,
                         @Nullable SubmissionStatusDetailBean statusDetailBean) {
        this.bassDTO = bassDTO;
        this.aggregation = aggregation;
        this.productOrders = productOrders;
        this.statusDetailBean = statusDetailBean;

        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setGroupingUsed(false);

        initializeFieldValues();
    }

    private void initializeFieldValues() {
        dateCompleted = null;
        if (bassDTO != null) {
            sample = bassDTO.getSample();
            datatype = bassDTO.getDatatype();
            project = bassDTO.getProject();
            fileTypeString = bassDTO.getFileType();
            version = bassDTO.getVersion();
            rpid = bassDTO.getRpid();
            fileName = bassDTO.getFileName();
            filePath = bassDTO.getPath();

        }
        if (aggregation != null) {
            String datatype = bassDTO.getDatatype();
            if (StringUtils.isNotBlank(datatype)) {
                qualityMetric = aggregation.getQualityMetric(datatype);
            }
            qualityMetricString = aggregation.getQualityMetricString(bassDTO.getDatatype());
            pctContamination = aggregation.getAggregationContam().getPctContamination();
            levelOfDetection = aggregation.getLevelOfDetection();
            if (levelOfDetection != null) {
                fingerprintLodMin = numberFormat.format(levelOfDetection.getMin());
                fingerprintLodMax = numberFormat.format(levelOfDetection.getMax());
            }
            readGroupCount = aggregation.getReadGroupCount();
            contaminationString = aggregation.getContaminationString();
        }
        initializeStatusDetailBean(statusDetailBean);
        submittedErrorsArray = submittedErrors.toArray(new String[submittedErrors.size()]);

        productOrdersString = new HashSet<>();
        if (productOrders != null) {
            for (ProductOrder productOrder : productOrders) {
                productOrdersString
                        .add(String.format("%s: %s", productOrder.getJiraTicketKey(), productOrder.getTitle()));
            }
        }
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
            submittedStatus = statusDetail.getStatus();
            if (statusDetail.getLastStatusUpdate() != null) {
                statusDate = DATE_FORMAT.format(statusDetail.getLastStatusUpdate());
            }
            BioProject project = statusDetail.getBioproject();
            if (project != null) {
                bioProject = String.format("%s %s", project.getAccession(), project.getAlias());
            }
            libraryDescriptor = statusDetail.getSubmissiondatatype();
            submissionSite = statusDetail.getSite();

        } else {
            submittedErrors = Collections.emptyList();
        }
    }

    public String getUuid() {
        return uuid;
    }

    public BassDTO getBassDTO() {
        return bassDTO;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public String getSampleName() {
        return sample;
    }

    public String getDataType() {
        return datatype;
    }

    public Collection<String> getProductOrdersString() {
        return productOrdersString;
    }

    public Collection<ProductOrder> getProductOrders() {
        return productOrders;
    }

    public String getAggregationProject() {
        return project;
    }

    public String getFileTypeString(){
        return fileTypeString;
    }

    @JsonIgnore
    public BassFileType getFileType() {
        return BassFileType.byBassValue(fileTypeString);
    }

    public int getVersion() {
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

    int getLanesInAggregation() {
        return readGroupCount;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
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

    @JsonIgnore
    public SubmissionTuple getSubmissionTuple() {
        return new SubmissionTuple(project, sample, String.valueOf(version), getFileType());
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

    public class SubmissionField {
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
    }
}
