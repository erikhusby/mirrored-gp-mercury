package org.broadinstitute.sequel.boundary.workflow;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * TODO: rename this to something other than ...Bean
 *
 * @author breilly
 */
@Named
public class WorkflowDescriptionBean {

    @Inject private DB db;

    public List<WorkflowDescription> getAllWorkflowDescriptions() {
        return db.getAllWorkflowDescriptions();
    }
}
