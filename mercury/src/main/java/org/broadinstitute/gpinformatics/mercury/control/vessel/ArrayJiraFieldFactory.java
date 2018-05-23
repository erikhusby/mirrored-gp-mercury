package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles creation of ARRAY tickets.
 */
public class ArrayJiraFieldFactory extends AbstractBatchJiraFieldFactory {

    private int sampleCount;
    private int emptyCount;
    private int controlCount;
    private LabVessel plate;

    ArrayJiraFieldFactory(@Nonnull LabBatch batch) {
        super(batch, CreateFields.ProjectType.ARRAY_PROJECT);
        LabVessel startingLabVessel = batch.getBucketEntries().iterator().next().getLabVessel();
        if (startingLabVessel.getType() != LabVessel.ContainerType.PLATE_WELL) {
            throw new RuntimeException("Expected plate well");
        }
        PlateWell plateWell = OrmUtil.proxySafeCast(startingLabVessel, PlateWell.class);
        plate = plateWell.getPlate();
        sampleCount = plate.getSampleInstanceCount();
        emptyCount = plate.getVesselGeometry().getVesselPositions().length - sampleCount;
    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {
        Set<CustomField> customFields = new HashSet<>();
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.DESCRIPTION, batch.getBatchDescription()));
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.NUMBER_OF_SAMPLES, sampleCount));
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.NUMBER_OF_EMPTIES, emptyCount));
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.NUMBER_OF_CONTROLS, controlCount));
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.NUMBER_OF_WELLS, sampleCount + emptyCount + controlCount));
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.CONTAINER_ID, plate.getLabel()));

        return customFields;
    }

    @Override
    public String generateDescription() {
        return "";
    }

    @Override
    public String getSummary() {
        // todo jmt plate name?
        return plate.getLabel();
    }
}
