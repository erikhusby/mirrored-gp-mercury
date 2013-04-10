package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.codehaus.jackson.annotate.JsonProperty;

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

    public ZamboniRead(Integer firstCycle,
                       Integer length,
                       String readType) {
        this.firstCycle = null;
        this.length = null;
        if (firstCycle != null) {
            this.firstCycle = ThriftConversionUtil.zeroAsNull(firstCycle);
        }
        if (length != null) {
            this.length = ThriftConversionUtil.zeroAsNull(length);
        }
        if (readType != null) {
            if (TEMPLATE.equals(readType)) {
                this.readType = ZamboniReadType.TEMPLATE;
            }
            else if (INDEX.equals(readType)) {
                this.readType = ZamboniReadType.INDEX;
            }
            else {
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
