package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.HashSet;

@XmlRootElement(name = "IlluminaRun")
public class ZimsIlluminaRun {
    
    @XmlAttribute(name = "name")
    private String runName;
    
    @XmlAttribute(name = "barcode")
    private String runBarcode;
    
    @XmlElement(name = "lane")
    private final Collection<ZimsIlluminaChamber> chambers = new HashSet<ZimsIlluminaChamber>();

    public ZimsIlluminaRun() {}

    public ZimsIlluminaRun(String runName,
                           String runBarcode) {
        this.runName = runName;
        this.runBarcode = runBarcode;
    }
    
    public String getRunName() {
        return runName;
    }
    
    public String getRunBarcode() {
        return runBarcode;
    }

    public void addChamber(ZimsIlluminaChamber chamber) {
        chambers.add(chamber);
    }
    
    public Collection<ZimsIlluminaChamber> getChambers() {
        return chambers;
    }


}
