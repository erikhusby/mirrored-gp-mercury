package org.broadinstitute.gpinformatics.athena.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/29/12
 * Time: 3:34 PM
 */
@XmlType(namespace = Namespaces.PRODUCT_NS)
@XmlEnum(String.class)
public enum ProductFamily {

    EXOME_EXPRESS,
    EXOME_SEQUENCING,
    EXOME_CHIP,
    WHOLE_GENOME_SEQUENCING,
    RNA_SEQUENCING,
    DENOVO_ASSEMBLY,
    FLUIDIGM;

}
