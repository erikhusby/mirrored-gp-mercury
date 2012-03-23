package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.control.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.control.quote.PriceItem;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.labevent.LabEventType;

import java.util.Map;

/**
 * A stub description of the end-to-end
 * workflow.  Things like "Hybrid Selection v8.0"
 * and "WGS 7.2"
 */
public class WorkflowDescription {

    private final String workflowName;

    private final String version;

    private Map<LabEventName,PriceItem> priceItemForEvent;

    private CreateIssueRequest.Fields.Issuetype issueType;

    /**
     * 
     * @param workflowName
     * @param version
     * @param billableEvents
     * @param issueType the <i>first</i> type of issue
     *                  created when this stuff hits
     *                  the lab.  Subsequent ticket
     *                  creation is owned by either {@link org.broadinstitute.sequel.entity.workflow.WorkflowEngine}
     *                  or {@link org.broadinstitute.sequel.entity.queue.LabWorkQueue}.
     */
    public WorkflowDescription(String workflowName,
                               String version,
                               Map<LabEventName,PriceItem> billableEvents,
                               CreateIssueRequest.Fields.Issuetype issueType) {
        if (workflowName == null) {
             throw new IllegalArgumentException("workflowName must be non-null in WorkflowDescription.WorkflowDescription");
        }
        this.workflowName = workflowName;
        this.version = version;
        this.priceItemForEvent = billableEvents;
        this.issueType = issueType;
    }
    
    public String getWorkflowName() {
        return workflowName;
    }
    
    public String getWorkflowVersion() {
        return version;
    }
    
    public PriceItem getPriceItem(LabEventName eventName) {
        if (eventName == null) {
            throw new NullPointerException("eventName cannot be null");
        }
        return priceItemForEvent.get(eventName);
    }

    public String getJiraProjectPrefix() {
        return "LCSET";
    }

    public CreateIssueRequest.Fields.Issuetype getJiraIssueType() {
        return issueType;
    }

}
