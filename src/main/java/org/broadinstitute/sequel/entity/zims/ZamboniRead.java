package org.broadinstitute.sequel.entity.zims;

import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ZamboniRead")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ZamboniRead {

    @XmlElement(name = "firstCycle")
    private Integer firstCycle;

    @XmlElement(name = "length")
    private Integer length;

    @XmlElement(name = "readType")
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
