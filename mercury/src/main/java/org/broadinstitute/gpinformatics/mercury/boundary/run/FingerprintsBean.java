package org.broadinstitute.gpinformatics.mercury.boundary.run;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * JAXB bean returned by FingerprintResource GET.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FingerprintsBean {
    private List<FingerprintBean> fingerprints;

    /** For JAXB. */
    @SuppressWarnings("unused")
    public FingerprintsBean() {
    }

    public FingerprintsBean(List<FingerprintBean> fingerprints) {
        this.fingerprints = fingerprints;
    }

    public List<FingerprintBean> getFingerprints() {
        return fingerprints;
    }
}
