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
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductResource;
import org.broadinstitute.gpinformatics.athena.presentation.products.WorkflowDiagrammer;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class supports all the actions done on workflows.
 */
@UrlBinding(WorkflowActionBean.ACTIONBEAN_URL_BINDING)
public class WorkflowActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/workflow/workflow.action";
    public static final String WORKFLOW_PARAMETER = "workflowDtoId";

    public static final String LIST_PAGE = "/workflow/list_workflow.jsp";
    public static final String VIEW_PAGE = "/workflow/view_workflow.jsp";

    public static final String GET_WORKFLOW_IMAGE_ACTION = "workflowImage";

    private final WorkflowLoader workflowLoader;

    // Combination of workflow def and one of its effective dates.
    public class WorkflowDefDateDto {
        public int id;
        public ProductWorkflowDef workflowDef;
        public Date effectiveDate;

        public WorkflowDefDateDto(int id, ProductWorkflowDef workflowDef, Date effectiveDate) {
            this.id = id;
            this.workflowDef = workflowDef;
            this.effectiveDate = effectiveDate;
        }

        public int getId() {
            return id;
        }

        public ProductWorkflowDef getWorkflowDef() {
            return workflowDef;
        }

        public Date getEffectiveDate() {
            return effectiveDate;
        }
    }

    // Data needed for displaying the list.
    private final List<WorkflowDefDateDto> allWorkflows;

    @Validate(required = true, on = {VIEW_ACTION})
    private WorkflowDefDateDto workflowDto;

    // Data needed for displaying the view.
    private WorkflowDefDateDto viewWorkflowDto;


    public List<WorkflowDefDateDto> getAllWorkflows() {
        return allWorkflows;
    }

    public WorkflowDefDateDto getViewWorkflowDto() {
        return viewWorkflowDto;
    }

    public ProductWorkflowDef getViewWorkflow() {
        return (viewWorkflowDto != null ? viewWorkflowDto.getWorkflowDef() : null);
    }

    public WorkflowActionBean() throws Exception {
        super(null, null, WORKFLOW_PARAMETER);

        workflowLoader = new WorkflowLoader();
        WorkflowConfig workflowConfig = workflowLoader.load();
        allWorkflows = new ArrayList<>();
        // Collects all workflows, each with possibly multiple effective dates.
        int id = 0;
        for (ProductWorkflowDef workflowDef : workflowConfig.getProductWorkflowDefs()) {
            for (Date date : workflowDef.getEffectiveDates()) {
                allWorkflows.add(new WorkflowDefDateDto(id++, workflowDef, date));
            }
        }
    }

    /**
     * Initialize the product with the passed in key for display in the form.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, GET_WORKFLOW_IMAGE_ACTION})
    public void init() {
        String workflowDtoId = getContext().getRequest().getParameter(WORKFLOW_PARAMETER);

        if (!StringUtils.isBlank(workflowDtoId)) {
            int id = Integer.valueOf(workflowDtoId);
            for (WorkflowDefDateDto dto : allWorkflows) {
                if (dto.getId() == id) {
                    viewWorkflowDto = dto;
                    break;
                }
            }
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

    /**
     * @return the path part of the Mercury REST url that returns the relevant workflow image file.
     */
    public String getWorkflowImage() {

        ProductWorkflowDef workflowDef = viewWorkflowDto.getWorkflowDef();
        Date effectiveDate = viewWorkflowDto.getEffectiveDate();

        File imageFile = new File(WorkflowDiagrammer.DIAGRAM_DIRECTORY,
                WorkflowDiagrammer.getWorkflowImageFileName(workflowDef, effectiveDate));

        return getContext().getRequest().getContextPath() + "/rest/" + ProductResource.WORKFLOW_DIAGRAM_IMAGE_URL +
               imageFile.getName();
    }
}
