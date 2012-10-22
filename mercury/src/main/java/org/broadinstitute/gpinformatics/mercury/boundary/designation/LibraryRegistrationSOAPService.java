package org.broadinstitute.gpinformatics.mercury.boundary.designation;

import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequelLibrary;

import java.io.Serializable;

/**
 * @author Scott Matthews
 *         Date: 6/27/12
 *         Time: 4:21 PM
 */
public interface LibraryRegistrationSOAPService extends Serializable {
    void registerSequeLLibrary(SequelLibrary registrationContextIn);

    void registerForDesignation(String libraryName, /*PassBackedProjectPlan projectPlanIn, */boolean needsControlLane);
}
