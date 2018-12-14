package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import java.util.Collection;

/**
 *  Holds Metadata for a given workflow, e.g for GDC submissions things that cannot be tracked in the lab
 *  accurately like 'library_prep_kit_catalog' but don't change very often based on the Workflow.
 */

@Audited
@Entity
public class WorkflowMetadata extends AttributeArchetype {
    public static final String WORKFLOW_METADATA = "Workflow Metadata";
    public static final String LIB_PREP_CATALOG_NUMBER = "library_preparation_kit_catalog_number";
    public static final String LIB_PREP_KIT_VENDOR = "library_preparation_kit_name";

    public WorkflowMetadata() {
    }

    public WorkflowMetadata(String mappingName, Collection<AttributeDefinition> attributeDefinitions) {
        super(WORKFLOW_METADATA, mappingName, attributeDefinitions);
    }

    public String getWorkflowName() {
        return getArchetypeName();
    }

}
