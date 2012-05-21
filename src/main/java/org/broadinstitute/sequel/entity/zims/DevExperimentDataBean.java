package org.broadinstitute.sequel.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DevExperimentDataBean {

    @JsonProperty("experiment")
    private String experiment;

    @JsonProperty("conditions")
    private List<String> conditions = new ArrayList<String>();

    public DevExperimentDataBean() {}

    public DevExperimentDataBean(TZDevExperimentData experimentData) {
        this.experiment = experimentData.getExperiment();
        conditions = new ArrayList<String>();
        for (String condition : experimentData.getConditionChain()) {
            if (condition != null) {
                if (condition.length() > 0) {
                    conditions.add(condition);
                }
            }
        }
    }

    public Collection<String> getConditions() {
        return conditions;
    }

    public String getExperiment() {
        return experiment;
    }


}
