package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/29/12
 * Time: 11:37 AM
 */
@XmlType(namespace = Namespaces.PRODUCT_NS)
@XmlEnum(String.class)
public enum GenomicsTechnology {

    ILLUMINA("Illumina"),
    FOUR_FIVE_FOUR("454"),
    ION_TORRENT("IonTorrent"),
    PACBIO("PacBio"),
    GENOTYPING_ILLUMINA("HT Genotyping Illumina");

    private String displayName;

    private GenomicsTechnology(final String displayName) {
        this.displayName = displayName;
    }

}
