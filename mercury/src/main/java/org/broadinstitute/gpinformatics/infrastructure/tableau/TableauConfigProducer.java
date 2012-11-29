package org.broadinstitute.gpinformatics.infrastructure.tableau;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.pmbridge.AbstractConfigProducer;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


public class TableauConfigProducer extends AbstractConfigProducer<TableauConfig> {

    @Inject
    private Deployment deployment;

    @Produces
    @Default
    public TableauConfig produce() {
        return produce(deployment);
    }

    public static TableauConfig getConfig(Deployment deployment) {
        return new TableauConfigProducer().produce(deployment);
    }
}
