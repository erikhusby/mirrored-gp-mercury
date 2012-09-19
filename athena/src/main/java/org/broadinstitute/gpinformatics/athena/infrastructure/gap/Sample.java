package org.broadinstitute.gpinformatics.athena.infrastructure.gap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 2/8/12
 * Time: 4:06 PM
 */
@XmlRootElement(name="sample")
public class Sample {
    
    private String name;
    private String plateName;
    private String  wellName;

    @XmlAttribute(name="name", required=true)
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name="plateName")
    public String getPlateName() {
        return plateName;
    }
    public void setPlateName(String plateName) {
        this.plateName = plateName;
    }

    @XmlAttribute(name="wellName")
    public String getWellName() {
        return wellName;
    }
    public void setWellName(String wellName) {
        this.wellName = wellName;
    }
}
