package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

@UrlBinding(value = "/workflow/AddRework.action")
public class AddReworkActionBean extends CoreActionBean {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private ReworkEjb reworkEjb;
    @Inject
    private LabEventHandler labEventHandler;

    private static final String FIND_VESSEL_ACTION = "viewVessel";
    private static final String VESSEL_INFO_ACTION = "vesselInfo";
    private static final String REWORK_SAMPLE_ACTION = "reworkSample";

    private String workflowName;
    private LabVessel labVessel;
    private List<WorkflowBucketDef> buckets;

    @Validate(required = true, on = {VESSEL_INFO_ACTION, REWORK_SAMPLE_ACTION})
    private String vesselLabel;

    @Validate(required = true, on = REWORK_SAMPLE_ACTION)
    private String bucketName;

    private WorkflowBucketDef bucket;

    @Validate(required = true, on = REWORK_SAMPLE_ACTION)
    private ReworkEntry.ReworkReason reworkReason;
    @Validate(required = true, on = REWORK_SAMPLE_ACTION)
    private String commentText;

    private static final String VIEW_PAGE = "/workflow/add_rework.jsp";
    private static final String VESSEL_INFO_PAGE = "/workflow/vessel_info.jsp";
    private LabEventType reworkStep;


    @HandlesEvent(REWORK_SAMPLE_ACTION)
    public Resolution reworkSample() {
        if (getBuckets().isEmpty()) {
            addValidationError("vesselLabel", "{2} is not in a bucket.", vesselLabel);
        }
        if (bucketName.equals("Pico/Plating Bucket")) {
            reworkStep = LabEventType.PICO_PLATING_BUCKET;
        } else {
            reworkStep = LabEventType.SHEARING_BUCKET;
        }
        try {
            reworkEjb.addRework(labVessel, reworkReason, reworkStep, commentText);
        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return view();
        }

        addMessage("Vessel {0} has been added to the {1} bucket.", labVessel.getLabel(), bucketName);
        return new RedirectResolution(this.getClass());
    }


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(FIND_VESSEL_ACTION)
    public Resolution findVessel() {
        labVessel = labVesselDao.findByIdentifier(vesselLabel);
        return new ForwardResolution(VIEW_PAGE);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {VESSEL_INFO_ACTION, REWORK_SAMPLE_ACTION})
    public void setUpLabVessel() {
        labVessel = labVesselDao.findByIdentifier(vesselLabel);
        if (labVessel != null) {
            for (SampleInstance sample : labVessel.getAllSamples()) {
                LabBatch labBatch = sample.getLabBatch();
                if (labBatch != null) {
                    workflowName = labBatch.getWorkflowName();
                    ProductWorkflowDefVersion workflowDef = labEventHandler.getWorkflowVersion(labBatch);
                    if (workflowName.equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                        buckets = workflowDef.getBuckets();
                    }
                    break;
                }
            }
        }
    }

    @HandlesEvent(VESSEL_INFO_ACTION)
    public ForwardResolution vesselInfo() throws Exception {
        if (labVessel == null) {
            addGlobalValidationError("Mercury does not recognize vessel with barcode {0}.", vesselLabel);
        }
        return new ForwardResolution(VESSEL_INFO_PAGE);
    }

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

    public WorkflowBucketDef getBucket() {
        return bucket;
    }

    public void setBucket(WorkflowBucketDef bucket) {
        this.bucket = bucket;
    }

    public ReworkEntry.ReworkReason getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(ReworkEntry.ReworkReason reworkReason) {
        this.reworkReason = reworkReason;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
