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

import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class SubmissionDto {
    public static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);
    private static final org.apache.commons.logging.Log log = LogFactory.getLog(SubmissionDto.class);
    private Collection<ProductOrder> productOrders;
    private SubmissionStatusDetailBean statusDetailBean;
    private final BassDTO bassDTO;
    private final Aggregation aggregation;

    public SubmissionDto(@Nonnull BassDTO bassDTO, Aggregation aggregation,
                         @Nonnull Collection<ProductOrder> productOrders,
                         @Nullable SubmissionStatusDetailBean statusDetailBean) {
        this.bassDTO = bassDTO;
        this.aggregation = aggregation;
        this.productOrders = productOrders;
        this.statusDetailBean = statusDetailBean;
    }

    public String getUuid() {
        String result = "";
        if (statusDetailBean != null) {
            result = statusDetailBean.getUuid();
        }
        return result;
    }

    public BassDTO getBassDTO() {
        return bassDTO;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    public String getSampleName() {
        return bassDTO.getSample();
    }

    public String getDataType() {
        return bassDTO.getDatatype();
    }

    public Collection<String> getProductOrdersString() {
        Collection<String> values = new HashSet<>(productOrders.size());
        for (ProductOrder productOrder : productOrders) {
            values.add(String.format("%s: %s", productOrder.getJiraTicketKey(), productOrder.getTitle()));
        }
        return values;
    }

    public Collection<ProductOrder> getProductOrders() {
        return productOrders;
    }

    String getAggregationProject() {
        return bassDTO.getProject();
    }

    String getFileTypeString() {
        return bassDTO.getFileType();
    }

    public BassFileType getFileType() {
        return BassFileType.byBassValue(bassDTO.getFileType());
    }

    public int getVersion() {
        return bassDTO.getVersion();
    }

    Double getQualityMetric() {
        return aggregation.getQualityMetric(bassDTO.getDatatype());
    }

    public String getQualityMetricString() {
        return aggregation.getQualityMetricString(bassDTO.getDatatype());
    }

    String getContaminationString() {
        return aggregation.getContaminationString();
    }

    Double getContamination() {
        return aggregation.getAggregationContam().getPctContamination();
    }

    public Date getDateCompleted() {
        return null;
    }

    LevelOfDetection getFingerprintLOD() {
        return aggregation.getLevelOfDetection();
    }

    public String getFingerprintLODString() {
        return getFingerprintLOD().toString();
    }

    public String getResearchProject() {
        return bassDTO.getRpid();
    }

    public int getLanesInAggregation() {
        return aggregation.getReadGroupCount();
    }

    public String getFileName() {
        return bassDTO.getFileName();
    }

    public String getFilePath() {
        return bassDTO.getPath();
    }

    public Integer getSubmittedVersion() {
        if (statusDetailBean != null) {
            return Integer.parseInt(statusDetailBean.getSubmittedVersion());
        }
        return null;
    }

    public List<String> getSubmittedErrors() {
        if (statusDetailBean != null) {
            return statusDetailBean.getErrors();
        }
        return Collections.emptyList();
    }

    public String[] getSubmittedErrorsArray() {
        List<String> errors = getSubmittedErrors();
        return errors.toArray(new String[errors.size()]);
    }

    public String getSubmittedStatus() {
        String status = "";
        if (statusDetailBean != null) {
            status = statusDetailBean.getStatus();
        }
        return status;
    }


    public String getStatusDate() {
        String statusDate = "";
        if (statusDetailBean != null && statusDetailBean.getLastStatusUpdate() != null) {
            statusDate = DATE_FORMAT.format(statusDetailBean.getLastStatusUpdate());
        }
        return statusDate;
    }

    public String getBioProject() {
        String bioProjectString = "";
        if (statusDetailBean != null && statusDetailBean.getBioproject() != null) {
            BioProject bioProject = statusDetailBean.getBioproject();
            bioProjectString = String.format("%s %s", bioProject.getAccession(), bioProject.getAlias());
        }
        return bioProjectString;
    }

    public String getSubmissionLibraryDescriptor() {
        String libraryDescriptor="";
        if (statusDetailBean != null) {
            libraryDescriptor = statusDetailBean.getLibraryDescriptor();
        }
        return libraryDescriptor;
    }

    public String getSubmissionRepositoryName() {
        String submissionSite = "";
        if (statusDetailBean != null) {
            submissionSite = statusDetailBean.getRepositoryName();
        }
        return submissionSite;
    }

    public void setStatusDetailBean(SubmissionStatusDetailBean statusDetailBean) {
        this.statusDetailBean = statusDetailBean;
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
        public static final String FINGERPRINT_LOD = "fingerprintLOD";
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
    }

    public SubmissionDisplayBean displayBean() {
        SubmissionDisplayBean submissionDisplayBean = new SubmissionDisplayBean();
        PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
        try {
            propertyUtilsBean.copyProperties(submissionDisplayBean, this);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("could not copy submissionDto properties", e);
        }
        return submissionDisplayBean;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    public class SubmissionDisplayBean implements Serializable {
        private static final long serialVersionUID = -1748300627900819095L;
        @JsonProperty(value = SubmissionDto.SubmissionField.SAMPLE_NAME)
        private String sampleName;

        @JsonProperty(value = SubmissionDto.SubmissionField.DATA_TYPE)
        private String dataType;

        @JsonProperty(value = SubmissionDto.SubmissionField.PRODUCT_ORDERS)
        private Collection<String> productOrdersString;

        @JsonProperty(value = SubmissionDto.SubmissionField.AGGREGATION_PROJECT)
        private String aggregationProject;

        @JsonProperty(value = SubmissionDto.SubmissionField.FILE_TYPE)
        private String fileTypeString;

        @JsonProperty(value = SubmissionDto.SubmissionField.VERSION)
        private int version;

        @JsonProperty(value = SubmissionDto.SubmissionField.QUALITY_METRIC)
        private String qualityMetricString;

        @JsonProperty(value = SubmissionDto.SubmissionField.CONTAMINATION_STRING)
        private String contaminationString;

        @JsonProperty(value = SubmissionDto.SubmissionField.FINGERPRINT_LOD)
        private String fingerprintLODString;

        @JsonProperty(value = SubmissionDto.SubmissionField.RESEARCH_PROJECT)
        private String researchProject;

        @JsonProperty(value = SubmissionDto.SubmissionField.LANES_IN_AGGREGATION)
        private int lanesInAggregation;

        @JsonProperty(value = SubmissionDto.SubmissionField.FILE_NAME)
        private String fileName;

        @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_VERSION)
        private Integer submittedVersion;

        @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_ERRORS)
        private List<String> submittedErrors;

        @JsonProperty(value = SubmissionDto.SubmissionField.SUBMITTED_STATUS)
        private String submittedStatus;

        @JsonProperty(value = SubmissionDto.SubmissionField.STATUS_DATE)
        private String statusDate;

        @JsonProperty(value = SubmissionDto.SubmissionField.BIO_PROJECT)
        private String bioProject;

        @JsonProperty(value = SubmissionDto.SubmissionField.LIBRARY_DESCRIPTOR)
        private String submissionLibraryDescriptor;

        @JsonProperty(value = SubmissionDto.SubmissionField.SUBMISSION_SITE)
        private String submissionRepositoryName;

        @JsonIgnore
        private String uuid;

        public String getAggregationProject() {
            return aggregationProject;
        }

        public void setAggregationProject(String aggregationProject) {
            this.aggregationProject = aggregationProject;
        }

        public String getBioProject() {
            return bioProject;
        }

        public void setBioProject(String bioProject) {
            this.bioProject = bioProject;
        }

        public String getContaminationString() {
            return contaminationString;
        }

        public void setContaminationString(String contaminationString) {
            this.contaminationString = contaminationString;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileTypeString() {
            return fileTypeString;
        }

        public void setFileTypeString(String fileTypeString) {
            this.fileTypeString = fileTypeString;
        }

        public String getFingerprintLODString() {
            return fingerprintLODString;
        }

        public void setFingerprintLODString(String fingerprintLODString) {
            this.fingerprintLODString = fingerprintLODString;
        }

        public int getLanesInAggregation() {
            return lanesInAggregation;
        }

        public void setLanesInAggregation(int lanesInAggregation) {
            this.lanesInAggregation = lanesInAggregation;
        }

        public Collection<String> getProductOrdersString() {
            return productOrdersString;
        }

        public void setProductOrdersString(Collection<String> productOrdersString) {
            this.productOrdersString = productOrdersString;
        }

        public String getQualityMetricString() {
            return qualityMetricString;
        }

        public void setQualityMetricString(String qualityMetricString) {
            this.qualityMetricString = qualityMetricString;
        }

        public String getResearchProject() {
            return researchProject;
        }

        public void setResearchProject(String researchProject) {
            this.researchProject = researchProject;
        }

        public String getSampleName() {
            return sampleName;
        }

        public void setSampleName(String sampleName) {
            this.sampleName = sampleName;
        }

        public String getStatusDate() {
            return statusDate;
        }

        public void setStatusDate(String statusDate) {
            this.statusDate = statusDate;
        }

        public String getSubmissionLibraryDescriptor() {
            return submissionLibraryDescriptor;
        }

        public void setSubmissionLibraryDescriptor(String submissionLibraryDescriptor) {
            this.submissionLibraryDescriptor = submissionLibraryDescriptor;
        }

        public String getSubmissionRepositoryName() {
            return submissionRepositoryName;
        }

        public void setSubmissionRepositoryName(String submissionRepositoryName) {
            this.submissionRepositoryName = submissionRepositoryName;
        }

        public List<String> getSubmittedErrors() {
            return submittedErrors;
        }

        public void setSubmittedErrors(List<String> submittedErrors) {
            this.submittedErrors = submittedErrors;
        }

        public String getSubmittedStatus() {
            return submittedStatus;
        }

        public void setSubmittedStatus(String submittedStatus) {
            this.submittedStatus = submittedStatus;
        }

        public Integer getSubmittedVersion() {
            return submittedVersion;
        }

        public void setSubmittedVersion(Integer submittedVersion) {
            this.submittedVersion = submittedVersion;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getUuid() {
            return uuid;
        }
    }
}
