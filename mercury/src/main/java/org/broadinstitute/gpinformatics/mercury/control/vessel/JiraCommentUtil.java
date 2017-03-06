package org.broadinstitute.gpinformatics.mercury.control.vessel;

// todo jmt re-evaluate where this functionality belongs

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.JiraTransitionType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.VesselSearchActionBean;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for sending human readable updates
 * about samples to project managers
 */
public class JiraCommentUtil {

    private final JiraService jiraService;

    private final AppConfig appConfig;

    private final BSPUserList bspUserList;

    private WorkflowConfig workflowConfig;

    private UserBean userBean;

    @Inject
    public JiraCommentUtil(JiraService jiraService, AppConfig appConfig, BSPUserList bspUserList,
                           WorkflowConfig workflowConfig, UserBean userBean) {
        this.jiraService = jiraService;
        this.appConfig = appConfig;
        this.bspUserList = bspUserList;
        this.workflowConfig = workflowConfig;
        this.userBean = userBean;
    }

    /**
     * Formats a message to record the receipt of a lab event
     *
     * @param labEvent event to add to JIRA ticket
     */
    public void postUpdate(LabEvent labEvent) {
        LabVessel messageLabVessel = null;
        Set<LabVessel> sourceLabVessels = labEvent.getSourceLabVessels();
        if (labEvent.getInPlaceLabVessel() == null) {
            if (!sourceLabVessels.isEmpty()) {
                messageLabVessel = sourceLabVessels.iterator().next();
            }
        } else {
            messageLabVessel = labEvent.getInPlaceLabVessel();
        }
        if (messageLabVessel != null) {
            // The label for a tube formation is a digest, so use the label for one of the tubes.
            if (OrmUtil.proxySafeIsInstance(messageLabVessel, TubeFormation.class)) {
                TubeFormation tubeFormation = OrmUtil.proxySafeCast(messageLabVessel, TubeFormation.class);
                messageLabVessel = tubeFormation.getContainerRole().getContainedVessels().iterator().next();
            }
            String message = "";
            if (bspUserList != null) {
                BspUser bspUser = bspUserList.getById(labEvent.getEventOperator());
                if (bspUser != null) {
                    message += bspUser.getUsername() + " ran ";
                }
            }
            message += labEvent.getLabEventType().getName() + " for <a href=\"" + appConfig.getUrl() +
                       VesselSearchActionBean.ACTIONBEAN_URL_BINDING + "?" + VesselSearchActionBean.VESSEL_SEARCH
                       + "=&searchKey=" +
                       messageLabVessel.getLabel() + "\">" + messageLabVessel.getLabel() + "</a>" +
                       " on " + labEvent.getEventLocation() + " at " + labEvent.getEventDate();

            Set<LabVessel> labVessels;
            if (labEvent.getInPlaceLabVessel() == null) {
                labVessels = sourceLabVessels;
            } else {
                labVessels = Collections.singleton(labEvent.getInPlaceLabVessel());
            }
            postUpdate(message, labVessels, labEvent);
        }
    }

    /**
     * Sends an alert with the message text to every project
     * in the sample sheet.  The message text is appended with
     * the complete list of samples per project so that the
     * final message looks something like this:
     * <p/>
     * "Applied adaptor ligation event to the following samples:
     * SM_2010
     * SM-515"
     * <p/>
     * Basically you'll want to do this fairly often to get
     * information rolled up and sent to the right project managers.
     *
     * @param message the text of the message
     * @param vessels the containers used in the operation
     */
    public void postUpdate(String message, Collection<LabVessel> vessels, @Nullable LabEvent labEvent) {
        Set<JiraTicket> tickets = new HashSet<>();
        Set<JiraTransitionType> transitions = new HashSet<>();
        for (LabVessel vessel : vessels) {
            // For cherry picks, update JIRA only for the tubes that are sources for transfers, not the entire rack.
            if (vessel.getContainerRole() != null && !vessel.getContainerRole().getContainedVessels().isEmpty() &&
                    labEvent != null && !labEvent.getCherryPickTransfers().isEmpty()) {
                for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers()) {
                    if (cherryPickTransfer.getSourceVesselContainer().equals(vessel.getContainerRole())) {
                        accumulateTickets(tickets, transitions,
                                vessel.getContainerRole().getVesselAtPosition(cherryPickTransfer.getSourcePosition()),
                                labEvent);
                    }
                }
            } else {
                accumulateTickets(tickets, transitions, vessel, labEvent);
            }
        }

        List<JiraIssue> jiraIssues = new ArrayList<>();
        for (JiraTicket ticket : tickets) {
            try {
                JiraIssue jiraIssue = ticket.getJiraDetails();
                jiraIssues.add(jiraIssue);
                if (ticket.getTicketName().startsWith("LCSET")) {
                    Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();
                    String fieldValue = (String) jiraIssue.getFieldValue(submissionFields.get(
                            LabBatch.TicketFields.LIMS_ACTIVITY_STREAM.getName()).getJiraCustomFieldId());
                    if (fieldValue == null) {
                        fieldValue = "";
                    }
                    fieldValue = fieldValue + "{html}" + message + "<br/>{html}";

                    CustomField mercuryUrlField = new CustomField(
                            submissionFields, LabBatch.TicketFields.LIMS_ACTIVITY_STREAM, fieldValue);

                    //TODO Comment out so as not to fill up the stupid tign for now
                    //jiraIssue.updateIssue(Collections.singleton(mercuryUrlField));
                }

                if (jiraIssue != null && jiraIssue.getKey() != null) {
                    String currentStatus = jiraIssue.getStatus();
                    //TODO Check for null?
                    String project = jiraIssue.getKey().split("-")[0];
                    for (JiraTransitionType transitionType : transitions) {
                        if (transitionType.getProject().equals(project)) {
                            if (currentStatus != null && transitionType.getEndStatus() != null &&
                                   !currentStatus.equals(transitionType.getEndStatus())) {
                                jiraIssue.postTransition(transitionType.getStatusTransition(),
                                        getUserName() + " transitioned to " + transitionType.getStatusTransition());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Accumulate JIRA tickets for vessels.
     */
    private void accumulateTickets(Set<JiraTicket> tickets, Set<JiraTransitionType> jiraTransitionTypes,
                                   LabVessel vessel, LabEvent labEvent) {
        for (SampleInstanceV2 sampleInstance : vessel.getSampleInstancesV2()) {
            LabBatch batch = sampleInstance.getSingleBatch();
            String workflowName = sampleInstance.getWorkflowName();
            if (batch != null && batch.getJiraTicket() != null) {
                tickets.add(batch.getJiraTicket());
                if (workflowName != null) {
                    ProductWorkflowDefVersion workflowVersion = workflowConfig.getWorkflowVersionByName(
                            workflowName, batch.getCreatedOn());
                    ProductWorkflowDefVersion.LabEventNode labEventNode =
                            workflowVersion.findStepByEventType(labEvent.getLabEventType().getName());
                    if (labEventNode != null) {
                        WorkflowStepDef workflowStepDef = labEventNode.getStepDef();
                        if (workflowStepDef != null && !workflowStepDef.getJiraTransition().isEmpty()) {
                            jiraTransitionTypes.addAll(workflowStepDef.getJiraTransition());
                        }
                    }
                }
            }
        }
    }

    /**
     * Sends an alert with the message text to every project
     * in the sample sheet.  The message text is appended with
     * the complete list of samples per project so that the
     * final message looks something like this:
     * <p/>
     * "Applied adaptor ligation event to the following samples:
     * SM_2010
     * SM-515"
     * <p/>
     * Basically you'll want to do this fairly often to get
     * information rolled up and sent to the right project managers.
     *
     * @param message the text of the message
     * @param vessel  the container used in the operation
     */
    public void postUpdate(String message, LabVessel vessel) {
        postUpdate(message, Collections.singleton(vessel), null);
    }

    /**
     * @return The name of the currently logged-in user or 'Mercury' if no logged in user (e.g. in a fixup test context).
     */
    private String getUserName() {
        String user = userBean.getLoginUserName();
        return user == null ? "Mercury" : user;
    }
}
