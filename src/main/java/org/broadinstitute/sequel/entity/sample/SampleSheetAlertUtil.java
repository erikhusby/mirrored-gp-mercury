package org.broadinstitute.sequel.entity.sample;



import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Utility methods for sending human readable alerts
 * about samples to project managers
 */
public class SampleSheetAlertUtil {

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
     * @param message the message text.
     * @param sampleSheet
     * @param includeDev if true, we'll include messages for
     * development projects.  if false, we'll skip alerting
     * dev projects.
     */
    public static void doAlert(String message, LabVessel labTangible,boolean includeDev) {
        // keep a list of sample names for each project because we're going
        // to make a single message that references each sample in a project
        final Map<Project,Collection<String>> samplesByProject = new HashMap<Project,Collection<String>>();
         for (SampleInstance samInstance: labTangible.getSampleInstances()) {
            Project p = samInstance.getProject();
            boolean useProject = true;
            if (p.isDevelopment() && !includeDev) {
                useProject = false;
            }
            if (useProject) {
                if (!samplesByProject.containsKey(p)) {
                    samplesByProject.put(p,new HashSet<String>());
                }
                samplesByProject.get(p).add(samInstance.getStartingSample().getSampleName());
            }
        }

        for (Map.Entry<Project,Collection<String>> entry: samplesByProject.entrySet()) {
            StringBuilder messageBuilder = new StringBuilder(message);
            messageBuilder.append("\n");
            for (String aliquotName: entry.getValue()) {
                messageBuilder.append(aliquotName).append("\n");
            }
            entry.getKey().getJiraTicket().addComment(messageBuilder.toString());
        }

    }

}
