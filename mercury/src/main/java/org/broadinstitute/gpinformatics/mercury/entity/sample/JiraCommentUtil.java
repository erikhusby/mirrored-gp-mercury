package org.broadinstitute.gpinformatics.mercury.entity.sample;



import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.project.*;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.*;

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

        final Set<JiraTicket> tickets = new HashSet<JiraTicket>();
        for (LabVessel vessel : vessels) {
            // todo arz talk to jmt about this.  I don't think I'm doing it right.
            if (OrmUtil.proxySafeIsInstance(vessel, VesselContainerEmbedder.class)) {
                VesselContainerEmbedder<? extends LabVessel> embedder = OrmUtil.proxySafeCast(vessel,VesselContainerEmbedder.class);
                for (VesselPosition position: embedder.getVesselContainer().getPositions()) {
                    Collection<LabBatch> batches = embedder.getVesselContainer().getNearestLabBatches(position);
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
                    tickets.add(jiraTicket);
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
        final Map<Project,Collection<String>> samplesByProject = new HashMap<Project,Collection<String>>();
        final Collection<StartingSample> allStarters = new HashSet<StartingSample>();

        for (LabVessel vessel : labVessels) {
            for (SampleInstance samInstance: vessel.getSampleInstances()) {
                for (ProjectPlan projectPlan : samInstance.getAllProjectPlans()) {
                    Project p = projectPlan.getProject();
                    if (!samplesByProject.containsKey(p)) {
                        samplesByProject.put(p,new HashSet<String>());
                    }
                    samplesByProject.get(p).add(samInstance.getStartingSample().getSampleName());
                    allStarters.add(samInstance.getStartingSample());
                }
            }
        }

        StringBuilder messageBuilder = new StringBuilder("{panel:title=Project Ownership}");
        messageBuilder.append("\n");
        messageBuilder.append("||Sample||Project||Owner||").append("\n");

        for (Map.Entry<Project,Collection<String>> entry: samplesByProject.entrySet()) {
            for (String sampleName: entry.getValue()) {
                String sampleURL = "[" + sampleName + "|http://gapqa01:8080/BSP/samplesearch/SampleSummary.action?sampleId=" + sampleName+ "]";
                Person projectOwner = entry.getKey().getPlatformOwner();
                String userName = null;
                if (projectOwner != null) {
                    userName = projectOwner.getLogin();
                }
                messageBuilder.append("|").append(sampleURL).append("|").append(entry.getKey().getProjectName()).append("|").append(userName).append("|").append("\n");
            }
        }
        messageBuilder.append("{panel}");
        ticket.addComment(messageBuilder.toString());
    }
}
