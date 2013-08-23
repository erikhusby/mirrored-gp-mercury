package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.util.List;

/**
 * This class supports all the actions done on workflows.
 */
@UrlBinding(WorkflowActionBean.ACTIONBEAN_URL_BINDING)
public class WorkflowActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/workflow/workflow.action";
    public static final String WORKFLOW_PARAMETER = "workflow";

    public static final String LIST_PAGE = "/workflow/list_workflow.jsp";
    public static final String VIEW_PAGE = "/workflow/view_workflow.jsp";

    public static final String GET_WORKFLOW_IMAGE_ACTION = "workflowImage";

    private final WorkflowLoader workflowLoader;

    // Data needed for displaying the list.
    private final List<ProductWorkflowDef> allWorkflows;

    @Validate(required = true, on = {VIEW_ACTION})
    private String workflowName;

    // Data needed for displaying the view.
    private ProductWorkflowDef viewWorkflow;

    public List<ProductWorkflowDef> getAllWorkflows() {
        return allWorkflows;
    }

    public ProductWorkflowDef getViewWorkflow() {
        return viewWorkflow;
    }

    public WorkflowActionBean() {
        super(null, null, WORKFLOW_PARAMETER);

        workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        allWorkflows = workflowConfig.getProductWorkflowDefs();
    }

    /**
     * Initialize the product with the passed in key for display in the form.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, GET_WORKFLOW_IMAGE_ACTION})
    public void init() {
        workflowName = getContext().getRequest().getParameter(WORKFLOW_PARAMETER);
        if (!StringUtils.isBlank(workflowName)) {
            WorkflowConfig workflowConfig = workflowLoader.load();
            viewWorkflow = workflowConfig.getWorkflowByName(workflowName);
        }
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * @return Show the create title if this is a developer or PDM.
     */
    @Override
    public boolean isCreateAllowed() {
        return false;
    }

    /**
     * @return Show the edit title if this is a developer or PDM.
     */
    @Override
    public boolean isEditAllowed() {
        return false;
    }
}
