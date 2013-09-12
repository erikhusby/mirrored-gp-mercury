package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
public class RegisterTubesBean {

    private List<RegisterTubeBean> registerTubeBeans = new ArrayList<>();

    public RegisterTubesBean() {
    }

    @XmlElement
    public List<RegisterTubeBean> getRegisterTubeBeans() {
        return registerTubeBeans;
    }

    public void setRegisterTubeBeans(List<RegisterTubeBean> registerTubeBeans) {
        this.registerTubeBeans = registerTubeBeans;
    }
}
