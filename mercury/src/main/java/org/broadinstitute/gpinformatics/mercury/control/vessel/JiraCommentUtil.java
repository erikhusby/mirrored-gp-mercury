package org.broadinstitute.gpinformatics.mercury.control.vessel;

// todo jmt this should be in control, or deleted

import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for sending human readable updates
 * about samples to project managers
 */
public class JiraCommentUtil {

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
    public static void postUpdate(String title,
                                  String message,
                                  Collection<LabVessel> vessels) {
        // keep a list of sample names for each project because we're going
        // to make a single message that references each sample in a project

        Set<JiraTicket> tickets = new HashSet<JiraTicket>();
        for (LabVessel vessel : vessels) {
            // todo arz talk to jmt about this.  I don't think I'm doing it right.
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
            StringBuilder messageBuilder = new StringBuilder("{panel:title=" + title + "}");
            if (message != null) {
                messageBuilder.append(message);
                messageBuilder.append("\n");
            }
            ticket.addComment(messageBuilder.toString());
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
    public static void postUpdate(String title,
                                  String message,
                                  LabVessel vessel) {
        Collection<LabVessel> vessels = new HashSet<LabVessel>();
        vessels.add(vessel);
        postUpdate(title,message,vessels);
        
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
