package org.broadinstitute.gpinformatics.mercury.entity.zims;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZamboniRead {

    @JsonProperty("firstCycle")
    private Integer firstCycle;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("readType")
    private ZamboniReadType readType;

    public static final String TEMPLATE = "TEMPLATE";

    public static final String INDEX = "INDEX";

    public ZamboniRead() {}

    /**
     * All fields are nullable because the pipeline may update values
     * at different points in the life of a run.
     *
     * See {@link ZimsIlluminaRun#addRead(edu.mit.broad.prodinfo.thrift.lims.TZamboniRead)}
     * for an explanation of how ZamboniReads are converted to thrift reads.
     * @param firstCycle nullable
     * @param length nullable
     * @param readType nullable
     */
    public ZamboniRead(Integer firstCycle,
                       Integer length,
                       String readType) {
        this.firstCycle = firstCycle;
        this.length = length;
        if (readType != null) {
            switch (readType) {
            case TEMPLATE:
                this.readType = ZamboniReadType.TEMPLATE;
                break;
            case INDEX:
                this.readType = ZamboniReadType.INDEX;
                break;
            default:
                throw new RuntimeException("Unknown read type: " + readType);
            }
        }
    }

    public Integer getFirstCycle() {
        return firstCycle;
    }

    public Integer getLength() {
        return length;
    }

    public ZamboniReadType getReadType() {
        return readType;
    }
}
