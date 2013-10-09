package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.vessel.generated.RegisterTubeBean;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement
@XmlType
public class RegisterTubesBean extends org.broadinstitute.gpinformatics.mercury.boundary.vessel.generated.RegisterTubesBean {
    @Override
    @XmlElement(name = "registerTubeBean")
    public List<RegisterTubeBean> getRegisterTubeBeans() {
        return super.getRegisterTubeBeans();
    }
}
