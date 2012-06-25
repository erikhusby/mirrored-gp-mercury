package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.entity.project.NumberOfLanesCoverage;
import org.broadinstitute.sequel.entity.project.PairedReadCoverage;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.infrastructure.squid.LibraryRegistrationSquidWSConnector;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigurationJNDIProfileDrivenImpl;

import javax.ejb.Stateless;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Scott Matthews
 *         Date: 6/20/12
 *         Time: 4:30 PM
 */
@Default
@Stateless
public class LibraryRegistrationSOAPService{

    @Inject
    LibraryRegistrationSquidWSConnector wsConnector;



    public void registerSequeLLibrary(SequelLibrary registrationContextIn) {
        this.wsConnector.squidCall().registerSequeLLibrary(registrationContextIn);
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

        this.wsConnector.squidCall().registerForDesignation(libraryName, lanes, readLength, needsControlLane);
    }
}
