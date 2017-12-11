package org.broadinstitute.gpinformatics.mercury.boundary.run;

import java.math.BigDecimal;

/**
 * JAXB DTO for posting a designation from Tableau.
 */
public class DesignationBean {
    private String tubeBarcode;
    private Integer numLanes;

    private String poolTestFlowcell;
    private Integer poolTestFlowcellLane;
    private Integer targetSize;
    private Integer targetCoverage;
    private Integer laneYield;
    private BigDecimal seqPenalty;

    public String getTubeBarcode() {
        return tubeBarcode;
    }

    public Integer getNumLanes() {
        return numLanes;
    }

    public String getPoolTestFlowcell() {
        return poolTestFlowcell;
    }

    public Integer getPoolTestFlowcellLane() {
        return poolTestFlowcellLane;
    }

    public Integer getTargetSize() {
        return targetSize;
    }

    public Integer getTargetCoverage() {
        return targetCoverage;
    }

    public Integer getLaneYield() {
        return laneYield;
    }

    public BigDecimal getSeqPenalty() {
        return seqPenalty;
    }
}
