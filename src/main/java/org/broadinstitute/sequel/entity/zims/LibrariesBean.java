package org.broadinstitute.sequel.entity.zims;

import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;

@XmlRootElement(name = "Libraries")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class LibrariesBean {

    @XmlElement(name = "libraries")
    private List<LibraryBean> libraries;
    
    public LibrariesBean() {}
    
    public LibrariesBean(List<LibraryBean> libraries) {
        this.libraries = libraries;
    }

    public Collection<LibraryBean> getLibraries() {
        return libraries;
    }
    
}
