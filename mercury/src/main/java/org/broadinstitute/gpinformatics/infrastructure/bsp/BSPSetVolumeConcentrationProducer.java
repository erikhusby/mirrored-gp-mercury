package org.broadinstitute.gpinformatics.infrastructure.bsp;


import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;

/**
 * This follows the Mercury guideline for producing test implementations of our services, described at:
 *
 * https://confluence.broadinstitute.org/display/SEQPLATINFX/Mercury+Configuration?focusedCommentId=44532773#comment-44532773
 *
 */
public class BSPSetVolumeConcentrationProducer {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    @ApplicationScoped
    public BSPSetVolumeConcentration produce(
            @New BSPSetVolumeConcentrationStub stub, @New BSPSetVolumeConcentrationImpl impl) {

        if (deployment == STUBBY) {
            return stub;
        }

        return impl;
    }

    public static BSPSetVolumeConcentration stubInstance() {
        return new BSPSetVolumeConcentrationStub();
    }

    /**
     * Creates a BSPSetVolumeConcentration with plain old new operator for container-free testing, not a managed bean!
     *
     * @return The volume and concentration BSP web service setter.
     */
    public static BSPSetVolumeConcentration testInstance() {
        BSPConfig bspConfig = BSPConfig.produce(DEV);
        return new BSPSetVolumeConcentrationImpl(bspConfig);
    }
}
