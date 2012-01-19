package org.broadinstitute.sequel;

/**
 * Abstraction around a jira ticket.
 */
public interface JiraTicket {

    public enum JiraResponse {
        OK,ERROR
    }

    /**
     * Because we're going to be calling this inline all over the
     * place, in performance-sensitive sections of code like
     * the message processor, we should think about having
     * a separate thread and thread queue to dispatch
     * jira comments
     * @param text
     */
    public JiraResponse addComment(String text);
}
