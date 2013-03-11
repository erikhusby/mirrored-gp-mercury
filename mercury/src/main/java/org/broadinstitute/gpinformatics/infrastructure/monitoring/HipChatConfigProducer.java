package org.broadinstitute.gpinformatics.infrastructure.monitoring;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class HipChatConfigProducer extends AbstractConfigProducer<HipChatConfig> {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    public HipChatConfig produce() {
        return produce(deployment);
    }

    public static HipChatConfig getConfig(Deployment deployment) {
        return new HipChatConfigProducer().produce(deployment);
    }
}
