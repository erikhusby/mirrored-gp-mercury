package org.broadinstitute.gpinformatics.mercury.boundary.designation;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.DeploymentProducer;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConfig;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidWebServiceClient;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.LibraryRegistrationPortType;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.SequelLibrary;

import javax.inject.Inject;

/**
 * @author Scott Matthews
 *         Date: 6/20/12
 *         Time: 4:30 PM
 */
@Impl
public class LibraryRegistrationSOAPServiceImpl extends SquidWebServiceClient<LibraryRegistrationPortType>
        implements LibraryRegistrationSOAPService {
    @Inject
    private DeploymentProducer deploymentProducer;

    private SquidConfig squidConfig;

    /**
     * Managed Bean classes must have no-arg constructor or constructor annotated @Initializer.
     */
    public LibraryRegistrationSOAPServiceImpl() {
    }

    /**
     * Deployment-specific constructor, not driven by MERCURY_DEPLOYMENT.
     *
     * @param deployment
     */
    public LibraryRegistrationSOAPServiceImpl(Deployment deployment) {
        squidConfig = SquidConfig.produce(deployment);
    }

    public LibraryRegistrationSOAPServiceImpl(SquidConfig squidConfig) {
        this.squidConfig = squidConfig;
    }

    @Override
    public void registerSequeLLibrary(SequelLibrary registrationContextIn) {
        squidCall().registerSequeLLibrary(registrationContextIn);
    }

    @Override
    public void registerForDesignation(String libraryName, /*PassBackedProjectPlan projectPlanIn,*/
                                       boolean needsControlLane) {
        int readLength = 0;
        int lanes = 0;

        // lanes = projectPlanIn.getLaneCoverage();
        // readLength = projectPlanIn.getReadLength();

        squidCall().registerForDesignation(libraryName, lanes, readLength, needsControlLane);
    }

    @Override
    protected SquidConfig getSquidConfig() {
        if (squidConfig == null) {
            final Deployment deployment = deploymentProducer.produce();
            squidConfig = SquidConfig.produce(deployment);
        }

        return squidConfig;
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
        return "/services/ExtLibraryRegistrationService?WSDL";
    }
}
