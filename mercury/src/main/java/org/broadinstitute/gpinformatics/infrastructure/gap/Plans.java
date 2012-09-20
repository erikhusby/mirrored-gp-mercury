package org.broadinstitute.gpinformatics.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="plans")
public class Plans {
    private List<ExperimentPlan> experimentPlans = new ArrayList<ExperimentPlan>();

    @XmlElement(name="experimentPlan")
    public List<ExperimentPlan> getExperimentPlans() {
        return experimentPlans;
    }

    public void setExperimentPlans(List<ExperimentPlan> experimentPlans) {
        this.experimentPlans = experimentPlans;
    }
}
    
