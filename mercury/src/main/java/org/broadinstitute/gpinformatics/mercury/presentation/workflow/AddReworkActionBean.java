package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.*;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

@UrlBinding(value = "/workflow/AddRework.action")
public class AddReworkActionBean extends CoreActionBean {
    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private AthenaClientService athenaClientService;

    @Inject
    private LabEventHandler labEventHandler;

    private String workflowName;
    private String vesselLabel;
    private String commentText;
    private LabVessel labVessel;
    private List<WorkflowBucketDef> buckets;
    private WorkflowBucketDef selectedBucket;

    private static final String VIEW_PAGE = "/resources/workflow/add_rework.jsp";

    public String getVesselLabel() {
        return vesselLabel;
    }

    public void setVesselLabel(String vesselLabel) {
        this.vesselLabel = vesselLabel;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public List<WorkflowBucketDef> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<WorkflowBucketDef> buckets) {
        this.buckets = buckets;
    }

    public WorkflowBucketDef getSelectedBucket() {
        return selectedBucket;
    }

    public void setSelectedBucket(WorkflowBucketDef selectedBucket) {
        this.selectedBucket = selectedBucket;
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("viewVessel")
    public Resolution findVessel() {
        labVessel = labVesselDao.findByIdentifier(vesselLabel);
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("vesselInfo")
    public ForwardResolution vesselInfo() {
        labVessel = labVesselDao.findByIdentifier(vesselLabel);
        if (labVessel != null) {
            for (SampleInstance sample : labVessel.getAllSamples()) {
                String productOrderKey = sample.getStartingSample().getProductOrderKey();
                if (StringUtils.isNotEmpty(productOrderKey)) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    workflowName = order.getProduct().getWorkflowName();
                    ProductWorkflowDefVersion workflowDef = labEventHandler.getWorkflowVersion(order.getBusinessKey());
                    buckets = workflowDef.getBuckets();
                    break;
                }
            }
        }
        return new ForwardResolution("/resources/workflow/vessel_info.jsp");
    }
}
