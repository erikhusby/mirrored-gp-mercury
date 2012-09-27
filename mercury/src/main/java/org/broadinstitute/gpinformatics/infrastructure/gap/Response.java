package org.broadinstitute.gpinformatics.infrastructure.gap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 2/8/12
 * Time: 4:12 PM
 */
@XmlRootElement(name="response")
public class Response {
    private List<ExperimentPlan> experimentPlans = new ArrayList<ExperimentPlan>();
    private Messages messages = new Messages();

    @XmlElement(name="experimentPlan")
    public List<ExperimentPlan> getExperimentPlans() {
        return experimentPlans;
    }
    public void setExperimentPlans(List<ExperimentPlan> experimentPlans) {
        this.experimentPlans = experimentPlans;
    }

    @XmlElement(name="messages")
    public Messages getMessages() {
        return messages;
    }
    public void setMessages(Messages messages) {
        this.messages = messages;
    }
}
