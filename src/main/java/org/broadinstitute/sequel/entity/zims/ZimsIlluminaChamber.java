package org.broadinstitute.sequel.entity.zims;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class ZimsIlluminaChamber {
    
    private String chamberName;

    private List<LibraryBean> libraries = new ArrayList<LibraryBean>();
    
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
