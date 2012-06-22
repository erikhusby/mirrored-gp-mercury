package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.entity.project.NumberOfLanesCoverage;
import org.broadinstitute.sequel.entity.project.PairedReadCoverage;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigurationJNDIProfileDrivenImpl;

import javax.enterprise.inject.Default;
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
public class LibraryRegistrationSOAPService{


    private SquidConfiguration squidConfiguration = new SquidConfigurationJNDIProfileDrivenImpl();


    private org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType squidServicePort;

    private org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType squidCall() {

        if (squidServicePort == null) {
            String namespace = "urn:ExtLibraryRegistration";
            QName serviceName = new QName(namespace, "ExtLibraryRegistrationService");

            String wsdlURL = squidConfiguration.getBaseURL() + "services/ExtLibraryRegistrationService?WSDL";

            URL url;
            try {
                url = new URL(wsdlURL);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            Service service = Service.create(url, serviceName);
            squidServicePort = service.getPort(serviceName, org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType.class);
        }

        return squidServicePort;

    }

    public void registerSequeLLibrary(SequelLibrary registrationContextIn) {
        this.squidCall().registerSequeLLibrary(registrationContextIn);
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

        this.squidCall().registerForDesignation(libraryName, lanes, readLength, needsControlLane);
    }
}
