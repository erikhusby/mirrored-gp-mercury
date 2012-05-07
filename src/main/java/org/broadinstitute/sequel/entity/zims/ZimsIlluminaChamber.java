package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.HashSet;

@XmlRootElement(name = "Lane")
public class ZimsIlluminaChamber {
    
    @XmlAttribute(name = "name")
    private String chamberName;

    @XmlElement(name = "libraries")
    private Collection<LibraryBean> libraries;
    
    public ZimsIlluminaChamber() {}
            
    public ZimsIlluminaChamber(short chamberName,final Collection<LibraryBean> libraries) {
        this.chamberName = Short.toString(chamberName);
        this.libraries = libraries;
    }
    
    public String getChamberName() {
        return chamberName;
    }
    
    public Collection<LibraryBean> getLibraries() {
        return libraries;
    }
}
