package org.broadinstitute.sequel.entity.sample;



import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

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
        final Map<Project,Collection<String>> samplesByProject = new HashMap<Project,Collection<String>>();
        final Collection<StartingSample> allStarters = new HashSet<StartingSample>();
        
        for (LabVessel vessel : vessels) {
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
        
        for (Map.Entry<Project,Collection<String>> entry: samplesByProject.entrySet()) {
            StringBuilder messageBuilder = new StringBuilder("{panel:title=" + title + "}");
            messageBuilder.append(message);
            messageBuilder.append("\n");
            
            int sampleCount = 0;
            for (String sampleName: entry.getValue()) {
                String sampleURL = "[" + sampleName + "|http://gapqa01:8080/BSP/samplesearch/SampleSummary.action?sampleId=" + sampleName+ "]";
                messageBuilder.append("|").append(sampleURL);
                sampleCount++;
                if (sampleCount % 6 == 0) {
                    messageBuilder.append("|").append("\n");
                }
            }
            if (!messageBuilder.toString().trim().endsWith("|")) {
                messageBuilder.append("|");
            }
            messageBuilder.append("\n");
            messageBuilder.append("h6.").append("There are " + (samplesByProject.keySet().size()) + " projects in this batch, representing " + allStarters.size() + " total samples.");
            messageBuilder.append("{panel}");
            // todo include total sample count from other projects, total sample count in batch.
            entry.getKey().addJiraComment(messageBuilder.toString());
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
                messageBuilder.append("|").append(sampleURL).append("|").append(entry.getKey().getProjectName()).append("|").append(entry.getKey().getPlatformOwner().getLogin()).append("|").append("\n");
            }
        }
        messageBuilder.append("{panel}");
        ticket.addComment(messageBuilder.toString());
    }
}
