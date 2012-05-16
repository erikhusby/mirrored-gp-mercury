package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.codehaus.jackson.annotate.JsonAutoDetect;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@XmlRootElement(name = "DevExperimentData")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class DevExperimentDataBean {

    @XmlElement(name = "experiment")
    private String experiment;

    @XmlElement(name = "conditionChain")
    private List<String> conditionData;

    public DevExperimentDataBean() {}

    public DevExperimentDataBean(TZDevExperimentData experimentData) {
        this.experiment = experimentData.getExperiment();
        conditionData = new ArrayList<String>();
        for (String condition : experimentData.getConditionChain()) {
            if (condition != null) {
                conditionData.add(condition);
            }
        }
    }

    public Collection<String> getConditions() {
        return conditionData;
    }


}
