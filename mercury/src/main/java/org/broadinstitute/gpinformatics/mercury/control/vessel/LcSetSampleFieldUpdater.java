package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Class that figures out how to write the GSSR samples
 * list into an lcset ticket
 */
public class LcSetSampleFieldUpdater {

    /**
     * Takes the initial samples and the rework samples
     * from the batch and builds a string to display
     * on the batch ticket
     * @param labBatch
     * @return
     */
    public String buildSamplesListString(LabBatch labBatch) {
        StringBuilder samplesText = new StringBuilder();
        for (LabVessel labVessel : labBatch.getStartingLabVessels()) {
            samplesText.append(labVessel.getLabel()).append("\n");
        }

        if (!labBatch.getReworks().isEmpty()) {
            samplesText.append("Reworks:\n");
            for (LabVessel rework : labBatch.getReworks()) {
                samplesText.append(rework.getLabel()).append(" (rework) ").append("\n");
            }
        }
        return samplesText.toString();
    }
}
