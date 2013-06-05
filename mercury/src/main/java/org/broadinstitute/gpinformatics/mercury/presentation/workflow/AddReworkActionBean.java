package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UrlBinding(value = "/workflow/AddRework.action")
public class AddReworkActionBean extends CoreActionBean {

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private ProductOrderSampleDao productOrderSampleDao;
    @Inject
    private ReworkEjb reworkEjb;
    @Inject
    private LabEventHandler labEventHandler;
    @Inject
    private WorkflowLoader workflowLoader;

    private static final String FIND_VESSEL_ACTION = "viewVessel";
    private static final String VESSEL_INFO_ACTION = "vesselInfo";
    private static final String REWORK_SAMPLE_ACTION = "reworkSample";

    private LabVessel labVessel;
    private List<ReworkEjb.ReworkCandidate> reworkCandidates = new ArrayList<>();
    private List<WorkflowBucketDef> buckets = new ArrayList<>();

    @Validate(required = true, on = {VESSEL_INFO_ACTION})
    private String vesselLabel;

    @Validate(required = true, on = REWORK_SAMPLE_ACTION)
    private String reworkBarcode;

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
        labVessel = labVesselDao.findByIdentifier(reworkBarcode);
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
        return new RedirectResolution(getClass());
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

    @Before(stages = LifecycleStage.BindingAndValidation, on = { VESSEL_INFO_ACTION, REWORK_SAMPLE_ACTION })
    public void initWorkflowBuckets() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        List<ProductWorkflowDef> workflowDefs = workflowConfig.getProductWorkflowDefs();
        //currently only do ExEx
        for (ProductWorkflowDef workflowDef : workflowDefs) {
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            if (workflowDef.getName().equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                buckets.addAll(workflowVersion.getBuckets());
            }
        }
        //set the initial bucket to the first in the list and load it
        if (buckets.size() > 0) {
            bucketName = buckets.get(0).getName();
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = VESSEL_INFO_ACTION)
    public void setUpLabVessel() {
        reworkCandidates = new ArrayList(reworkEjb.findReworkCandidates(vesselLabel));
    }

    @HandlesEvent(VESSEL_INFO_ACTION)
    public ForwardResolution vesselInfo() {
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

    public List<ReworkEjb.ReworkCandidate> getReworkCandidates() {
        return reworkCandidates;
    }

    public String getReworkBarcode() {
        return reworkBarcode;
    }

    public void setReworkBarcode(String reworkBarcode) {
        this.reworkBarcode = reworkBarcode;
    }
}
