package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;

// Implements Serializable because it's used by a Stateful session bean.
@Dependent
public class LabEventHandler implements Serializable {

    public enum HandlerResponse {
        OK,
        ERROR /* further refine to "out of order", "bad molecular envelope", critical, warning, etc. */
    }

    private static final Log LOG = LogFactory.getLog(LabEventHandler.class);


    @Inject
    private JiraCommentUtil jiraCommentUtil;

    public LabEventHandler() {
    }

    public HandlerResponse processEvent(LabEvent labEvent) {

        // Updates JIRA with the event info.
        if (jiraCommentUtil != null) {
            try {
                jiraCommentUtil.postUpdate(labEvent);
            } catch (Exception e) {
                // This is not fatal, so don't rethrow
                LOG.error("Failed to update JIRA", e);
            }
        }

        return HandlerResponse.OK;
    }

}
