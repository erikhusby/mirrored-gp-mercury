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

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SubmissionDto {
    public static final FastDateFormat DATE_FORMAT =
            FastDateFormat.getDateTimeInstance(FastDateFormat.MEDIUM, FastDateFormat.SHORT);
    private Collection<ProductOrder> productOrders;
    private final Collection<SubmissionStatusDetailBean> statusDetailBean;

    public BassDTO getBassDTO() {
        return bassDTO;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    private final BassDTO bassDTO;
    private final Aggregation aggregation;
    private double contamination = 0;

    public SubmissionDto(BassDTO bassDTO, Aggregation aggregation, Collection<ProductOrder> productOrders,
                         Collection<SubmissionStatusDetailBean> statusDetailBean) {
        this.bassDTO = bassDTO;
        this.aggregation = aggregation;
        this.productOrders = productOrders;
        this.statusDetailBean = statusDetailBean;
    }


    public String getSampleName() {
        return bassDTO.getSample();
    }

    public String getBioSample() {
        return null;
    }

    public String getDataType() {
        return bassDTO.getDatatype();
    }

    public Collection<ProductOrder> getProductOrders() {
        return productOrders;
    }

    public String getAggregationProject() {
        return bassDTO.getProject();
    }

    public String getFileType() {
        return bassDTO.getFileType();
    }

    public int getVersion() {
        return bassDTO.getVersion();
    }

    public Double getQualityMetric() {
        return aggregation.getQualityMetric(bassDTO.getDatatype());
    }

    public String getQualityMetricString() {
        return aggregation.getQualityMetricString(bassDTO.getDatatype());
    }

    public String getContaminationString() {
        return aggregation.getContaminationString();
    }

    public Double getContamination() {
        return aggregation.getAggregationContam().getPctContamination();
    }

    public Date getDateCompleted() {
        return null;
    }

    public LevelOfDetection getFingerprintLOD() {
        return aggregation.getLevelOfDetection();
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

    public String getSubmittedStatus() {
        Set<String> stati = new HashSet<>();
        for (SubmissionStatusDetailBean submissionStatusDetailBean : statusDetailBean) {
            String status = String.format("%s: %s", submissionStatusDetailBean.getUuid(),
                    submissionStatusDetailBean.getStatus());
            stati.add(status);
        }
        return stati.toString();
    }

    public String getStatusDate() {
        Set<String> stati = new HashSet<>();
        for (SubmissionStatusDetailBean submissionStatusDetailBean : statusDetailBean) {
            String statusDate = DATE_FORMAT.format(submissionStatusDetailBean.getLastStatusUpdate());
            String statusDateString = String.format("%s: %s", submissionStatusDetailBean.getUuid(), statusDate);
            stati.add(statusDateString);
        }
        return stati.toString();
    }
}
