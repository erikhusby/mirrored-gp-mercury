package org.broadinstitute.gpinformatics.mercury.boundary.designation;

import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequelLibrary;
import org.broadinstitute.gpinformatics.mercury.entity.project.PassBackedProjectPlan;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Stub;

/**
 * @author Scott Matthews
 *         Date: 6/27/12
 *         Time: 4:23 PM
 */
@Stub
public class LibraryRegistrationSOAPServiceStub implements LibraryRegistrationSOAPService {

    public LibraryRegistrationSOAPServiceStub() {
    }

    @Override
    public void registerSequeLLibrary(SequelLibrary registrationContextIn) {

    }

    @Override
    public void registerForDesignation(String libraryName, PassBackedProjectPlan projectPlanIn,
                                       boolean needsControlLane) {

    }
}
