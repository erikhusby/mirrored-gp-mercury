package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.codehaus.jackson.annotate.JsonProperty;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

    @JsonProperty("sequencedLibraryCreationTime")
    private String creationTime;

    private static SimpleDateFormat creationTimeDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public ZimsIlluminaChamber() {}

    public ZimsIlluminaChamber(short chamberName,
                                   final List<LibraryBean> libraries,
                                   final String primer,
                                   final String sequencedLibraryName,
                               String creationTime) {
        this.chamberName = Short.toString(chamberName);
        this.libraries = libraries;
        this.primer = primer;
        this.sequencedLibraryName = sequencedLibraryName;
        this.creationTime = creationTime;
    }

    public ZimsIlluminaChamber(short chamberName,
                               final List<LibraryBean> libraries,
                               final String primer,
                               final String sequencedLibraryName,
                               final Date creationTime) {
        this(chamberName, libraries, primer, sequencedLibraryName, creationTimeDateFormat.format(creationTime));
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

    public String getCreationTime() {
        return creationTime;
    }
}
