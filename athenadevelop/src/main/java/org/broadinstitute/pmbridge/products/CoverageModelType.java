package org.broadinstitute.pmbridge.products;

import org.broadinstitute.pmbridge.Namespaces;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:05 AM
 */
@XmlType(namespace = Namespaces.PRODUCT_NS)
@XmlEnum
public enum CoverageModelType {

    LANES("Lanes"),
    DEPTH("Depth"),
    TARGETCOVERAGE("Target Coverage"),
    PFREADS("PF Reads"),
    MEANTARGETCOVERAGE("Mean Target Coverage");

    private final String fullName;

    private CoverageModelType(final String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
