package org.broadinstitute.gpinformatics.mercury.entity.zims;


import com.fasterxml.jackson.annotation.JsonProperty;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DevExperimentDataBean {


    @JsonProperty("experiment")
    private String experiment;

    @JsonProperty("conditions")
    private List<String> conditions = new ArrayList<>();

    public DevExperimentDataBean() {}

    public DevExperimentDataBean(TZDevExperimentData experimentData, JiraService jiraService) {
        this.experiment = experimentData.getExperiment();
        conditions = new ArrayList<>();
        if(jiraService != null) {
            try {
                for (String condition : experimentData.getConditionChain()) {
                    if (!condition.isEmpty()) {
                        JiraIssue jiraIssue = jiraService.getIssueInfo(condition);
                        if(this.experiment == null) {
                            this.experiment = jiraIssue.getParent();
                        }
                        if(jiraIssue != null) {
                           conditions.add(jiraIssue.getSummary());
                        }
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(String.format("Error accessing Jira Service: %s", e.getMessage()));
            }
        }
        else {
            for (String condition : experimentData.getConditionChain()) {
                if (condition != null) {
                    if (condition.length() > 0) {
                        conditions.add(condition);
                    }
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
