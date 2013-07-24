package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ZimsIlluminaChamber {

    @JsonProperty("name")
    private String chamberName;

    @JsonProperty("libraries")
    private List<LibraryBean> libraries = new ArrayList<>();

    @JsonProperty("primer")
    private String primer;

    @JsonProperty("sequencedLibrary")
    private String sequencedLibraryName;
    
    public ZimsIlluminaChamber() {}
            
    public ZimsIlluminaChamber(short chamberName,
                               final List<LibraryBean> libraries,
                               final String primer,
                               final String sequencedLibraryName) {
        this.chamberName = Short.toString(chamberName);
        this.libraries = libraries;
        this.primer = primer;
        this.sequencedLibraryName = sequencedLibraryName;
    }
    
    public String getPrimer() {
        return primer;
    }
    
    public String getName() {
        return chamberName;
    }

    public Collection<LibraryBean> getLibraries() {
        return libraries;
    }
    
    public String getSequencedLibrary() {
        return this.sequencedLibraryName;
    }
}
