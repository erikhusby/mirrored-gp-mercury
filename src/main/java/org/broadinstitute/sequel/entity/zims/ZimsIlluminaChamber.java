package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.HashSet;

@XmlRootElement(name = "Lane")
public class ZimsIlluminaChamber {
    
    @XmlElement(name = "name")
    private String chamberName;

    @XmlElement(name = "library")
    private Collection<LibraryBean> libraries;
    
    @XmlElement(name = "primer")
    private String primer;
    
    public ZimsIlluminaChamber() {}
            
    public ZimsIlluminaChamber(short chamberName,
                               final Collection<LibraryBean> libraries,
                               final String primer) {
        this.chamberName = Short.toString(chamberName);
        this.libraries = libraries;
        this.primer = primer;
    }
    
    public String getPrimer() {
        return primer;
    }
    
    public String getChamberName() {
        return chamberName;
    }
    
    public Collection<LibraryBean> getLibraries() {
        return libraries;
    }
}
