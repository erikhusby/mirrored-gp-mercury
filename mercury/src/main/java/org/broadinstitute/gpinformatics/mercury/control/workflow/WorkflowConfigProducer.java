package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * Need the WorkflowLoader to be a singleton EJB referenced by this CDI instance so
 * WildFly CDI injection and EJB threads don't cause:
 * concurrent access timeout on [instance] - could not obtain lock within 5000 MILLISECONDS
 */
@Dependent
public class WorkflowConfigProducer {

    @Inject
    WorkflowLoader loader;

    @Produces
    @Default
    public WorkflowConfig produce() {
        return loader.load();
    }
}
