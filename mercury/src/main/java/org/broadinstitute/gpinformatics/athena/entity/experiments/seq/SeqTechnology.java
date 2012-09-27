package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/11/12
 * Time: 5:01 PM
 */
public enum SeqTechnology {

    ILLUMINA("Illumina"),
    FOUR_FIVE_FOUR("454"),
    ION_TORRENT("Ion Torrent"),
    PACBIO("PacBio");

    private String displayName;

    private SeqTechnology(String name) {
        this.displayName = name;
    }

    public String value() {
        return name();
    }

    public static SeqTechnology fromValue(String v) {
        return valueOf(v);
    }

    public String getDisplayName() {
        return displayName;
    }

}
