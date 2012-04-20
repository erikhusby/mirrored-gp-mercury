package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Collection;

@XmlRootElement(name = "DevExperimentData")
@XmlAccessorType(XmlAccessType.FIELD)
public class DevExperimentDataBean {

    private String experiment;

    private Collection<String> conditionData;

    public DevExperimentDataBean() {}

    public DevExperimentDataBean(TZDevExperimentData experimentData) {
        this.experiment = experimentData.getExperiment();
        this.conditionData = experimentData.getConditionChain();
    }



}
