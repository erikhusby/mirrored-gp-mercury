package org.broadinstitute.sequel.boundary.labevent;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * A JAX-RS DTO for a list of LabEventBeans
 */
@XmlRootElement
public class LabEventResponseBean {
    private List<LabEventBean> labEventBeans = new ArrayList<LabEventBean>();

    public LabEventResponseBean(List<LabEventBean> labEventBeans) {
        this.labEventBeans = labEventBeans;
    }

    /** For JAXB */
    public LabEventResponseBean() {
    }

    public List<LabEventBean> getLabEventBeans() {
        return labEventBeans;
    }

    public void setLabEventBeans(List<LabEventBean> labEventBeans) {
        this.labEventBeans = labEventBeans;
    }
}
