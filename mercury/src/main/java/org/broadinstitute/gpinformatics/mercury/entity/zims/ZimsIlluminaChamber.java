package org.broadinstitute.gpinformatics.mercury.entity.zims;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.time.FastDateFormat;

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

    @JsonProperty("loadingConcentration")
    private Double loadingConcentration;

    @JsonProperty("actualReadStructure")
    private String actualReadStructure;

    @JsonProperty("setupReadStructure")
    private String setupReadStructure;

    private static FastDateFormat creationTimeDateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

    public ZimsIlluminaChamber() {}

    public ZimsIlluminaChamber(
            short chamberName,
            final List<LibraryBean> libraries,
            final String primer,
            final String sequencedLibraryName,
            String creationTime,
            Double loadingConcentration,
            String actualReadStructure) {
        this.chamberName = Short.toString(chamberName);
        this.libraries = libraries;
        this.primer = primer;
        this.sequencedLibraryName = sequencedLibraryName;
        this.creationTime = creationTime;
        this.loadingConcentration = loadingConcentration;
        this.actualReadStructure = actualReadStructure;
    }

    public ZimsIlluminaChamber(
            short chamberName,
            final List<LibraryBean> libraries,
            final String primer,
            final String sequencedLibraryName,
            final Date creationTime,
            Double loadingConcentration,
            String actualReadStructure,
            String setupReadStructure) {
        this(chamberName, libraries, primer, sequencedLibraryName, creationTimeDateFormat.format(creationTime),
                loadingConcentration, actualReadStructure, setupReadStructure);
    }

    public ZimsIlluminaChamber(
            short chamberName,
            final List<LibraryBean> libraries,
            final String primer,
            final String sequencedLibraryName,
            final String creationTime,
            Double loadingConcentration,
            String actualReadStructure,
            String setupReadStructure) {
        this.chamberName = Short.toString(chamberName);
        this.libraries = libraries;
        this.primer = primer;
        this.sequencedLibraryName = sequencedLibraryName;
        this.creationTime = creationTime;
        this.loadingConcentration = loadingConcentration;
        this.actualReadStructure = actualReadStructure;
        this.setupReadStructure = setupReadStructure;
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

    public Double getLoadingConcentration() {
        return loadingConcentration;
    }

    public String getActualReadStructure() {
        return actualReadStructure;
    }

    public String getSetupReadStructure() {
        return setupReadStructure;
    }
}
