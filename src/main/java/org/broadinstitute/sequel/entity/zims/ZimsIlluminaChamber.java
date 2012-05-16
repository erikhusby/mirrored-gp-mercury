package org.broadinstitute.sequel.entity.zims;

import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@XmlRootElement(name = "Lane")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class ZimsIlluminaChamber {
    
    @XmlElement(name = "name")
    private String chamberName;

    @XmlElement(name = "library")
    private List<LibraryBean> libraries;
    
    @XmlElement(name = "primer")
    private String primer;
    
    public ZimsIlluminaChamber() {}
            
    public ZimsIlluminaChamber(short chamberName,
                               final List<LibraryBean> libraries,
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
