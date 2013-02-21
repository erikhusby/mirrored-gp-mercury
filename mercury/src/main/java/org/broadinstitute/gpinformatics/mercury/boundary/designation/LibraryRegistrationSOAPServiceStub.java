package org.broadinstitute.gpinformatics.mercury.boundary.designation;

import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequelLibrary;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;

/**
 * @author Scott Matthews
 *         Date: 6/27/12
 *         Time: 4:23 PM
 */
@Stub
@Alternative
public class LibraryRegistrationSOAPServiceStub implements LibraryRegistrationSOAPService {

    public LibraryRegistrationSOAPServiceStub() {
    }

    @Override
    public void registerSequeLLibrary(SequelLibrary registrationContextIn) {

    }

    @Override
    public void registerForDesignation(String libraryName, /*PassBackedProjectPlan projectPlanIn,*/
                                       boolean needsControlLane) {

    }
}
