package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.entity.project.NumberOfLanesCoverage;
import org.broadinstitute.sequel.entity.project.PairedReadCoverage;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.infrastructure.squid.AbstractSquidWSConnector;
import org.broadinstitute.sequel.infrastructure.squid.SquidConnectionParameters;

import javax.ejb.Stateless;
import javax.enterprise.inject.Default;
import javax.inject.Inject;


/**
 * @author Scott Matthews
 *         Date: 6/20/12
 *         Time: 4:30 PM
 */

@Default
@Stateless
public class LibraryRegistrationSOAPService extends AbstractSquidWSConnector<LibraryRegistrationPortType> {


    @Inject
    private SquidConnectionParameters squidConnectionParameters;


    public void registerSequeLLibrary(SequelLibrary registrationContextIn) {
        squidCall().registerSequeLLibrary(registrationContextIn);
    }

    public void registerForDesignation(String libraryName, PassBackedProjectPlan projectPlanIn,
                                       boolean needsControlLane) {

        int readLength = 0;
        int lanes = 0;

        for(SequencingPlanDetail projPlanDetail:projectPlanIn.getPlanDetails()) {
            if(projPlanDetail.getCoverageGoal() instanceof NumberOfLanesCoverage) {
                lanes = Integer.parseInt(projPlanDetail.getCoverageGoal().coverageGoalToParsableText());
            } else if(projPlanDetail.getCoverageGoal() instanceof PairedReadCoverage) {
                readLength = Integer.parseInt(projPlanDetail.getCoverageGoal().coverageGoalToParsableText());
            }
        }

        squidCall().registerForDesignation(libraryName, lanes, readLength, needsControlLane);
    }

    @Override
    protected String getBaseUrl() {
        return squidConnectionParameters.getBaseUrl();
    }

    @Override
    protected String getNameSpace() {
        return "urn:ExtLibraryRegistration";
    }

    @Override
    protected String getServiceName() {
        return "ExtLibraryRegistrationService";
    }

    @Override
    protected String getWsdlLocation() {
        return "services/ExtLibraryRegistrationService?WSDL";
    }
}
