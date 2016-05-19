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
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class SubmissionDto {
    public static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);
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

    String getFileTypeString(){
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

    int getLanesInAggregation() {
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
            String submittedVersion = statusDetailBean.getSubmittedVersion();
            if (StringUtils.isNotBlank(submittedVersion)) {
                return Integer.parseInt(submittedVersion);
            }
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
            bioProjectString = formatBioProject(statusDetailBean.getBioproject());
        }
        return bioProjectString;
    }

    protected static String formatBioProject(BioProject bioProject) {
        return String.format("%s %s", bioProject.getAccession(), bioProject.getAlias());
    }

    String getSubmissionLibraryDescriptor() {
        String libraryDescriptor="";
        if (statusDetailBean != null) {
            libraryDescriptor = statusDetailBean.getSubmissiondatatype();
        }
        return libraryDescriptor;
    }

    String getSubmissionRepositoryName() {
        String submissionSite = "";
        if (statusDetailBean != null) {
            submissionSite = statusDetailBean.getSite();
        }
        return submissionSite;
    }

    public void setStatusDetailBean(SubmissionStatusDetailBean statusDetailBean) {
        this.statusDetailBean = statusDetailBean;
    }

    public SubmissionData submissionData() {
        return new SubmissionData(this);
    }

    public SubmissionStatusDetailBean getStatusDetailBean() {
        return statusDetailBean;
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
        public static final String UUID = "uuid";
    }
}
