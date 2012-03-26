package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.project.JiraTicket;

public class JiraLabWorkQueueResponse implements LabWorkQueueResponse {

    private final String text;

    private final JiraTicket ticket;

    public JiraLabWorkQueueResponse(String text,
                                    JiraTicket ticket) {
        if (text == null) {
            throw new NullPointerException("text cannot be null");
        }
        if (ticket == null) {
            throw new RuntimeException("jira ticket cannot be null");
        }
        this.text = text;
        this.ticket = ticket;
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    public JiraTicket getJiraTicket() {
        return ticket;
    }
}
