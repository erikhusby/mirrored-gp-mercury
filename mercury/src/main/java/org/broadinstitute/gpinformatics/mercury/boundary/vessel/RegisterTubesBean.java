package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlType
public class RegisterTubesBean {

    private List<RegisterTubeBean> registerTubeBeans = new ArrayList<>();

    public RegisterTubesBean() {
    }

    @XmlElement(name="registerTubeBean")
    public List<RegisterTubeBean> getRegisterTubeBeans() {
        return registerTubeBeans;
    }

    public void setRegisterTubeBeans(List<RegisterTubeBean> registerTubeBeans) {
        this.registerTubeBeans = registerTubeBeans;
    }
}
