package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FCTJiraFieldFactory extends AbstractBatchJiraFieldFactory {

    public FCTJiraFieldFactory(@Nonnull LabBatch batch) {
        super(batch, CreateFields.ProjectType.FCT_PROJECT);
    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {
        Set<CustomField> customFields = new HashSet<>();

        //TODO not sure if true. At this point we can still by loading old batches
        List<LabBatchStartingVessel> startingVessels = new ArrayList<>(batch.getLabBatchStartingVessels());
        Collections.sort(startingVessels, new Comparator<LabBatchStartingVessel>() {
            @Override
            public int compare(LabBatchStartingVessel startingVessel, LabBatchStartingVessel startingVessel2) {
                if (startingVessel.getVesselPosition() == null || startingVessel2.getVesselPosition() == null) {
                    throw new RuntimeException("VesselPosition not set on LabBatchStartingVessel");
                }
                return startingVessel.getVesselPosition().compareTo(startingVessel2.getVesselPosition());
            }
        });
        StringBuilder laneInfoBuilder = new StringBuilder();
        laneInfoBuilder.append("||Lane||Loading Vessel||Loading Concentration||\n");
        for (LabBatchStartingVessel startingVessel : startingVessels) {
            laneInfoBuilder.append("|");
            laneInfoBuilder.append(startingVessel.getVesselPosition().name());
            laneInfoBuilder.append("|");
            laneInfoBuilder.append(startingVessel.getLabVessel().getLabel());
            laneInfoBuilder.append("|");
            laneInfoBuilder.append(startingVessel.getConcentration());
            laneInfoBuilder.append("|");
            laneInfoBuilder.append("\n");
        }
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.LANE_INFO,
                laneInfoBuilder.toString()));

        return customFields;
    }

    @Override
    public String generateDescription() {
        return null;
    }

    @Override
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        for (LabVessel vessel : batch.getStartingBatchLabVessels()) {
            summary.append(vessel.getLabel());
        }
        return summary.toString();
    }
}
