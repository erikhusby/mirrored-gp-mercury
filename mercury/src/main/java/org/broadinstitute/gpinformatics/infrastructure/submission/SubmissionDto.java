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

import com.sun.istack.Nullable;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class SubmissionDto {
    public static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);
    private Collection<ProductOrder> productOrders;
    private final SubmissionStatusDetailBean statusDetailBean;
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

    @JsonIgnore
    public String getUuid() {
        String result = "";
        if (statusDetailBean != null) {
            result = statusDetailBean.getUuid();
        }
        return result;
    }

    @JsonIgnore
    public BassDTO getBassDTO() {
        return bassDTO;
    }

    @JsonIgnore
    public Aggregation getAggregation() {
        return aggregation;
    }

    @JsonProperty(value = SubmissionField.SAMPLE_NAME)
    public String getSampleName() {
        return bassDTO.getSample();
    }

    @JsonProperty(value = SubmissionField.DATA_TYPE)
    public String getDataType() {
        return bassDTO.getDatatype();
    }

    @JsonProperty(value = SubmissionField.PRODUCT_ORDERS)
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

    @JsonProperty(value = SubmissionField.AGGREGATION_PROJECT)
    public String getAggregationProject() {
        return bassDTO.getProject();
    }

    @JsonProperty(value = SubmissionField.FILE_TYPE)
    public String getFileTypeString(){
        return getFileType().getValue();
    }

    @JsonIgnore
    public BassDTO.FileType getFileType() {
        return bassDTO.getFileType();
    }

    @JsonProperty(value = SubmissionField.VERSION)
    public int getVersion() {
        return bassDTO.getVersion();
    }

    @JsonIgnore
    public Double getQualityMetric() {
        return aggregation.getQualityMetric(bassDTO.getDatatype());
    }

    @JsonProperty(value = SubmissionField.QUALITY_METRIC)
    public String getQualityMetricString() {
        return aggregation.getQualityMetricString(bassDTO.getDatatype());
    }

    @JsonProperty(value = SubmissionField.CONTAMINATION_STRING)
    public String getContaminationString() {
        return aggregation.getContaminationString();
    }

    public Double getContamination() {
        return aggregation.getAggregationContam().getPctContamination();
    }

    @JsonIgnore
    public Date getDateCompleted() {
        return null;
    }

    @JsonIgnore
    public LevelOfDetection getFingerprintLOD() {
        return aggregation.getLevelOfDetection();
    }

    @JsonProperty(value = SubmissionField.FINGERPRINT_LOD)
    public String getFingerprintLODString() {
        return getFingerprintLOD().toString();
    }

    @JsonProperty(value = SubmissionField.RESEARCH_PROJECT)
    public String getResearchProject() {
        return bassDTO.getRpid();
    }

    @JsonProperty(value = SubmissionField.LANES_IN_AGGREGATION)
    public int getLanesInAggregation() {
        return aggregation.getReadGroupCount();
    }

    @JsonProperty(value = SubmissionField.FILE_NAME)
    public String getFileName() {
        return bassDTO.getFileName();
    }

    @JsonIgnore
    public String getFilePath() {
        return bassDTO.getPath();
    }

    @JsonProperty(value = SubmissionField.SUBMITTED_VERSION)
    public Integer getSubmittedVersion() {
        if (statusDetailBean != null) {
            return Integer.parseInt(statusDetailBean.getSubmittedVersion());
        }
        return null;
    }

    @JsonProperty(value = SubmissionField.SUBMITTED_ERRORS)
    public List<String> getSubmittedErrors() {
        if (statusDetailBean != null) {
            return statusDetailBean.getErrors();
        }
        return Collections.emptyList();
    }

    @JsonIgnore
    public String[] getSubmittedErrorsArray() {
        List<String> errors = getSubmittedErrors();
        return errors.toArray(new String[errors.size()]);
    }

    @JsonProperty(value = SubmissionField.SUBMITTED_STATUS)
    public String getSubmittedStatus() {
        String status = "";
        if (statusDetailBean != null) {
            status = statusDetailBean.getStatus();
        }
        return status;
    }


    @JsonProperty(value = SubmissionField.STATUS_DATE)
    public String getStatusDate() {
        String statusDate = "";
        if (statusDetailBean != null && statusDetailBean.getLastStatusUpdate() != null) {
            statusDate = DATE_FORMAT.format(statusDetailBean.getLastStatusUpdate());
        }
        return statusDate;
    }

    @JsonProperty(value = SubmissionField.BIO_PROJECT)
    public String getBioProject() {
        String bioproject = "";
        if (statusDetailBean != null && statusDetailBean.getBioproject() != null) {
            bioproject = statusDetailBean.getBioproject().getAccession();
        }
        return bioproject;
    }

    public BassFileType getFileTypeEnum() {
        return BassFileType.byBassValue(getFileType());
    }

    @JsonProperty(value = SubmissionField.LIBRARY_DESCRIPTOR)
    public String getSubmissionLibraryDescriptor() {
        String libraryDescriptor="";
        if (statusDetailBean != null) {
            libraryDescriptor = statusDetailBean.getLibraryDescriptor();
        }
        return libraryDescriptor;
    }

    @JsonProperty(value = SubmissionField.SUBMISSION_SITE)
    public String getSubmissionRepositoryName() {
        String submissionSite = "";
        if (statusDetailBean != null) {
            submissionSite = statusDetailBean.getSubmissionRepositoryName();
        }
        return submissionSite;
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
}
