package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;

import java.io.Serializable;
import java.util.Set;

public interface BSPCohortSearchService extends Serializable {

    /* Method to retrieve all of the cohorts associated with a particular BSP user. The available columns of data are
     * Collection ID
     * Collection Name
     * Collection Category
     * Group Name
     * Archived
     */
    Set<Cohort> getAllCohorts();

}
