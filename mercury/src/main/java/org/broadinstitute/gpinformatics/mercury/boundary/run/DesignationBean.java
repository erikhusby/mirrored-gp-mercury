package org.broadinstitute.gpinformatics.mercury.boundary.run;

import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

/**
 * JAXB DTO for posting a designation from Tableau.
 */
@XmlRootElement
public class DesignationBean {
    private String tubeBarcode;
    private Integer numLanes;

    private String poolTestFlowcell;
    private Integer poolTestFlowcellLane;
    private Integer targetSize;
    private Integer targetCoverage;
    private Integer laneYield;
    private BigDecimal seqPenalty;

    /** For JAXB. */
    public DesignationBean() {
    }

    public String getTubeBarcode() {
        return tubeBarcode;
    }

    public void setTubeBarcode(String tubeBarcode) {
        this.tubeBarcode = tubeBarcode;
    }

    public Integer getNumLanes() {
        return numLanes;
    }

    public void setNumLanes(Integer numLanes) {
        this.numLanes = numLanes;
    }

    public String getPoolTestFlowcell() {
        return poolTestFlowcell;
    }

    public void setPoolTestFlowcell(String poolTestFlowcell) {
        this.poolTestFlowcell = poolTestFlowcell;
    }

    public Integer getPoolTestFlowcellLane() {
        return poolTestFlowcellLane;
    }

    public void setPoolTestFlowcellLane(Integer poolTestFlowcellLane) {
        this.poolTestFlowcellLane = poolTestFlowcellLane;
    }

    public Integer getTargetSize() {
        return targetSize;
    }

    public void setTargetSize(Integer targetSize) {
        this.targetSize = targetSize;
    }

    public Integer getTargetCoverage() {
        return targetCoverage;
    }

    public void setTargetCoverage(Integer targetCoverage) {
        this.targetCoverage = targetCoverage;
    }

    public Integer getLaneYield() {
        return laneYield;
    }

    public void setLaneYield(Integer laneYield) {
        this.laneYield = laneYield;
    }

    public BigDecimal getSeqPenalty() {
        return seqPenalty;
    }

    public void setSeqPenalty(BigDecimal seqPenalty) {
        this.seqPenalty = seqPenalty;
    }
}
