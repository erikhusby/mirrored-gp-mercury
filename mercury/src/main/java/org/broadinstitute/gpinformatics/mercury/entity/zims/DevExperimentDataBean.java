package org.broadinstitute.gpinformatics.mercury.entity.zims;


import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DevExperimentDataBean {

    @JsonProperty("experiment")
    private String experiment;

    @JsonProperty("conditions")
    private List<String> conditions = new ArrayList<>();

    public DevExperimentDataBean() {}

    public DevExperimentDataBean(TZDevExperimentData experimentData) {
        this.experiment = experimentData.getExperiment();
        conditions = new ArrayList<>();
        for (String condition : experimentData.getConditionChain()) {
            if (condition != null) {
                if (condition.length() > 0) {
                    conditions.add(condition);
                }
            }
        }
    }

    public String getExperiment() {
        return experiment;
    }

    public Collection<String> getConditions() {
        return conditions;
    }
}
