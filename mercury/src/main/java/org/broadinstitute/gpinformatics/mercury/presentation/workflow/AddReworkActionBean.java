package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkLevel;
import org.broadinstitute.gpinformatics.mercury.entity.rework.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@UrlBinding(value = "/workflow/AddRework.action")
public class AddReworkActionBean extends CoreActionBean {
    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private AthenaClientService athenaClientService;

    @Inject
    private LabEventHandler labEventHandler;

    private static final String FIND_VESSEL = "viewVessel";
    private static final String VESSEL_INFO = "vesselInfo";
    private static final String REWORK_SAMPLE = "reworkSample";

    private String workflowName;
    private LabVessel labVessel;
    private List<WorkflowBucketDef> buckets;

    @Validate(required = true, on = {VESSEL_INFO, REWORK_SAMPLE})
    private String vesselLabel;

    @Validate(required = true, on = REWORK_SAMPLE)
    private String bucketName;

    private WorkflowBucketDef bucket;

    @Validate(required = true, on = REWORK_SAMPLE)
    private ReworkReason reworkReason;
    @Validate(required = true, on = REWORK_SAMPLE)
    private String commentText;

    private static final String VIEW_PAGE = "/resources/workflow/add_rework.jsp";
    private static final String VESSEL_INFO_PAGE = "/resources/workflow/vessel_info.jsp";
    private LabEventType reworkStep;


    @HandlesEvent(REWORK_SAMPLE)
    public Resolution reworkSample() {
        if (getBuckets().isEmpty()) {
            addValidationError("vesselLabel", "{0} is not in a bucket.", vesselLabel);
        }

        List<MercurySample> reworks = new ArrayList<MercurySample>();
        for (VesselContainer vesselContainer : labVessel.getContainerList()) {
            boolean skipVessel = false;
            for (VesselPosition vesselPosition : vesselContainer.getEmbedder().getVesselGeometry()
                    .getVesselPositions()) {
                if (!skipVessel) {
                    final Collection<SampleInstance> samplesAtPosition =
                            labVessel.getSamplesAtPosition(vesselPosition.name());

                    for (SampleInstance sampleInstance : samplesAtPosition) {
                        final MercurySample mercurySample = sampleInstance.getStartingSample();

                        final BucketEntry bucketEntry =
                                bucketEntryDao.findByVesselAndPO(labVessel, mercurySample.getProductOrderKey());
                        if (bucketEntry != null) {
                            skipVessel = true;
                            addGlobalValidationError("Sample {2} in product order {3} already exists in the {4} bucket.",
                                    mercurySample.getSampleKey(), mercurySample.getProductOrderKey(),
                                    bucketEntry.getBucket().getBucketDefinitionName());
                        }
                        if (!reworks.contains(mercurySample) && skipVessel) {
                            mercurySample.reworkSample(
                                    reworkReason, ReworkLevel.ONE_SAMPLE_RELEASE_REST_BATCH,
                                    labVessel.getLatestEvent(),
                                    reworkStep, labVessel, vesselPosition, commentText
                            );
                            reworks.add(mercurySample);
                        }
                    }
                }
            }
        }
        if (!reworks.isEmpty()) {
            mercurySampleDao.persistAll(reworks);
        }
        return new ForwardResolution(VIEW_PAGE);
    }


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(FIND_VESSEL)
    public Resolution findVessel() {
        labVessel = labVesselDao.findByIdentifier(vesselLabel);
        return new ForwardResolution(VIEW_PAGE);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {REWORK_SAMPLE})
    public void setUpLabRework() {
//        bucket=bucketDao.findByName(bucket.getName())
        if (bucketName.equals("Pico/Plating Bucket")) {
            reworkStep = LabEventType.PICO_PLATING_BUCKET;
        } else {
            reworkStep = LabEventType.SHEARING_BUCKET;
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {VESSEL_INFO, REWORK_SAMPLE})
    public void setUpLabVessel() {
        labVessel = labVesselDao.findByIdentifier(vesselLabel);
        if (labVessel != null) {
            for (SampleInstance sample : labVessel.getAllSamples()) {
                String productOrderKey = sample.getStartingSample().getProductOrderKey();
                if (StringUtils.isNotEmpty(productOrderKey)) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    workflowName = order.getProduct().getWorkflowName();
                    ProductWorkflowDefVersion workflowDef = labEventHandler.getWorkflowVersion(order.getBusinessKey());
                    if (workflowName.equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                        buckets = workflowDef.getBuckets();
                    }
                    break;
                }
            }
        }
    }

    @HandlesEvent(VESSEL_INFO)
    public ForwardResolution vesselInfo() {
        if (labVessel==null){
            addGlobalValidationError("Mercury does not recognize vessel with barcode {0}.",vesselLabel);
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

    public ReworkReason getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(ReworkReason reworkReason) {
        this.reworkReason = reworkReason;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
