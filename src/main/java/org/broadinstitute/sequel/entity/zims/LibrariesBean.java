package org.broadinstitute.sequel.entity.zims;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;

@XmlRootElement(name = "Libraries")
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
