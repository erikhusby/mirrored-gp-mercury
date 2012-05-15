package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(name = "DevExperimentData")
@XmlAccessorType(XmlAccessType.FIELD)
public class DevExperimentDataBean {

    @XmlElement(name = "experiment")
    private String experiment;

    @XmlElementWrapper(name = "conditionChain")
    private ArrayList<String> conditionData;

    public DevExperimentDataBean() {}

    public DevExperimentDataBean(TZDevExperimentData experimentData) {
        this.experiment = experimentData.getExperiment();
        conditionData = new ArrayList<String>();
        conditionData.addAll(experimentData.getConditionChain());
    }

    public Collection<String> getConditions() {
        return conditionData;
    }


}
