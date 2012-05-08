package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ZamboniRead")
public class ZamboniRead {

    @XmlAttribute(name = "firstCycle")
    private Integer firstCycle;

    @XmlAttribute(name = "length")
    private Integer length;

    @XmlAttribute(name = "readType")
    private ZamboniReadType readType;

    public static final String TEMPLATE = "TEMPLATE";

    public static final String INDEX = "INDEX";

    public ZamboniRead() {}

    public ZamboniRead(Integer firstCycle,
                       Integer length,
                       String readType) {
        this.firstCycle = ThriftConversionUtil.zeroAsNull(firstCycle);
        this.length = ThriftConversionUtil.zeroAsNull(length);
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
