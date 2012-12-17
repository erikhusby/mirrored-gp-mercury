package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;

import java.io.Serializable;
import java.util.Set;

public interface BSPMaterialTypeService extends Serializable {

    /* Method to retrieve all of the material types associated with a particular BSP user. The available columns of data are
     * Collection ID
     * Collection Name
     * Collection Category
     * Group Name
     * Archived
     */
    Set<MaterialType> getAllMaterialTypes();

}
