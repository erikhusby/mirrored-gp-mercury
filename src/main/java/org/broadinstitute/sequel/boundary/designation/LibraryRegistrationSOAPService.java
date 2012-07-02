package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;

import java.io.Serializable;

/**
 * @author Scott Matthews
 *         Date: 6/27/12
 *         Time: 4:21 PM
 */
public interface LibraryRegistrationSOAPService extends Serializable {
    void registerSequeLLibrary(SequelLibrary registrationContextIn);

    void registerForDesignation(String libraryName, PassBackedProjectPlan projectPlanIn, boolean needsControlLane);
}
