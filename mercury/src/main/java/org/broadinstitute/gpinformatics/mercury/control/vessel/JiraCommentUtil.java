package org.broadinstitute.gpinformatics.mercury.control.vessel;

// todo jmt re-evaluate where this functionality belongs

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for sending human readable updates
 * about samples to project managers
 */
public class JiraCommentUtil {

    private JiraService jiraService;

    @Inject
    public JiraCommentUtil(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    /**
     * Sends an alert with the message text to every project
     * in the sample sheet.  The message text is appended with
     * the complete list of samples per project so that the
     * final message looks something like this:
     *
     * "Applied adaptor ligation event to the following samples:
     * SM_2010
     * SM-515"
     *
     * Basically you'll want to do this fairly often to get
     * information rolled up and sent to the right project managers.
     * @param  message the text of the message
     * @param vessels the containers used in the operation
     */
    public void postUpdate(String message,
                Collection<LabVessel> vessels) {
        // keep a list of sample names for each project because we're going
        // to make a single message that references each sample in a project

        Set<JiraTicket> tickets = new HashSet<JiraTicket>();
        for (LabVessel vessel : vessels) {
            VesselContainer vesselContainer = vessel.getContainerRole();
            if (vesselContainer != null) {
                for (Object o: vesselContainer.getPositions()) {
                    VesselPosition position = (VesselPosition) o;
                    Collection<LabBatch> batches = vesselContainer.getNearestLabBatches(position);
                    if (batches != null) {
                        for (LabBatch batch : batches) {
                            if (batch.getJiraTicket() != null) {
                                tickets.add(batch.getJiraTicket());
                            }
                        }
                    }
                }
            }
            else {
                for (LabBatch labBatch : vessel.getLabBatches()) {
                    JiraTicket jiraTicket = labBatch.getJiraTicket();
                    if(jiraTicket != null) {
                        tickets.add(jiraTicket);
                    }
                }
            }
        }
        for (JiraTicket ticket : tickets) {
            try {
                JiraIssue jiraIssue = ticket.getJiraDetails();
                Map<String, CustomFieldDefinition> submissionFields = jiraService.getCustomFields();
                String fieldValue = (String) jiraIssue.getFieldValue(submissionFields.get("LIMS Activity Stream").getJiraCustomFieldId());
                if(fieldValue == null) {
                    fieldValue = "";
                }
                fieldValue = new StringBuilder().append(fieldValue).append("{html}").append(message).append("<br/>{html}").toString();

                CustomField mercuryUrlField = new CustomField(
                        submissionFields, LabBatch.RequiredSubmissionFields.LIMS_ACTIVITY_STREAM, fieldValue);

                jiraIssue.updateIssue(Collections.singleton(mercuryUrlField));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Sends an alert with the message text to every project
     * in the sample sheet.  The message text is appended with
     * the complete list of samples per project so that the
     * final message looks something like this:
     *
     * "Applied adaptor ligation event to the following samples:
     * SM_2010
     * SM-515"
     *
     * Basically you'll want to do this fairly often to get
     * information rolled up and sent to the right project managers.
     * @param  message the text of the message
     * @param vessel the container used in the operation
     */
    public void postUpdate(String message,
                LabVessel vessel) {
        Collection<LabVessel> vessels = new HashSet<LabVessel>();
        vessels.add(vessel);
        postUpdate(message, vessels);
        
    }

    public static void postProjectOwnershipTableToTicket(Collection<LabVessel> labVessels,
                                                   JiraTicket ticket) {
        // keep a list of sample names for each project because we're going
        // to make a single message that references each sample in a project
        Collection<MercurySample> allStarters = new HashSet<MercurySample>();

        for (LabVessel vessel : labVessels) {
            for (SampleInstance samInstance: vessel.getSampleInstances()) {
                allStarters.add(samInstance.getStartingSample());
            }
        }

        StringBuilder messageBuilder = new StringBuilder("{panel:title=Project Ownership}");
        messageBuilder.append("\n");
        messageBuilder.append("||Sample||Project||Owner||").append("\n");

        messageBuilder.append("{panel}");
        ticket.addComment(messageBuilder.toString());
    }
}
