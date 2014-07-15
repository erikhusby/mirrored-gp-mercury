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

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.metrics.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;

import java.util.List;

public class SubmissionDTO {
    private List<String> productOrderIds;

    public BassDTO getBassDTO() {
        return bassDTO;
    }

    public Aggregation getAggregation() {
        return aggregation;
    }

    private final BassDTO bassDTO;
    private final Aggregation aggregation;

    public SubmissionDTO(BassDTO bassDTO, Aggregation aggregation, List<String> productOrderIds) {

        this.bassDTO = bassDTO;
        this.aggregation = aggregation;
        this.productOrderIds = productOrderIds;
    }


    public String getSample() {
        return bassDTO.getSample();
    }

    public String getBioSample() {
        return null;
    }

    public String getDataType() {
        return bassDTO.getDatatype();
    }

    public List<String> getProductOrderIds() {
        return productOrderIds;
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

    public String getQualityMetric() {
        return null;
    }

    public double getContamination() {
        return aggregation.getAggregationContam().getPctContamination();
    }

    public LevelOfDetection getFingerprintLOD() {
        LevelOfDetection.calculate(aggregation.getAggregationReadGroups());
        return aggregation.getLevelOfDetection();
    }

    public long getLanes() {
        long lanes=0;
        for (AggregationReadGroup aggregationReadGroup : aggregation.getAggregationReadGroups()) {
            lanes += aggregationReadGroup.getLane();
        }
        return lanes;
    }

    public String getResearchProject() {
        return bassDTO.getRpid();
    }
}
