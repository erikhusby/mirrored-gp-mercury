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
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet.ReworkEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.ReworkReason;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UrlBinding(value = AddReworkActionBean.ADD_REWORK_ACTION_BEAN)
public class AddReworkActionBean extends CoreActionBean {

    public static final String OTHER_REASON_REFERENCE = "Other...";
    public static final String ADD_REWORK_ACTION_BEAN = "/workflow/AddToBucket.action";
    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ReworkEjb reworkEjb;

    @Inject
    private LabEventHandler labEventHandler;

    @Inject
    private ReworkReasonDao reworkReasonDao;

    private static final String FIND_VESSEL_ACTION = "viewVessel";
    private static final String VESSEL_INFO_ACTION = "vesselInfo";
    private static final String ADD_SAMPLE_ACTION = "addSample";

    private LabVessel labVessel;
    private List<ReworkEjb.BucketCandidate> bucketCandidates = new ArrayList<>();
    private List<WorkflowBucketDef> buckets = new ArrayList<>();

    @Validate(required = true, on = {VESSEL_INFO_ACTION})
    private String vesselLabel;

    private int numQueryInputs;

    private List<String> noResultQueryTerms = new ArrayList<>();

    @Validate(required = true, on = ADD_SAMPLE_ACTION)
    private List<String> selectedBucketCandidates;

    private Set<String> selectedReworkVessels = new HashSet<>();

    @Validate(required = true, on = ADD_SAMPLE_ACTION)
    private String bucketName;

    private WorkflowBucketDef bucket;

    private String reworkReason;

    private String userReworkReason;

    private String commentText;

    private static final String VIEW_PAGE = "/workflow/add_to_bucket.jsp";
    private static final String VESSEL_INFO_PAGE = "/workflow/vessel_info.jsp";

    /**
     * Since the functionality of this page supports both rework and non rework vessels, validation of the mandatory
     * rework fields now needs to be conditional based on if any of the selected vessels are indicated to be rework
     * vessels.
     */
    @ValidationMethod(on = ADD_SAMPLE_ACTION)
    public void validateAddSampleInput() {
        if (StringUtils.isEmpty(bucketName)) {
            addValidationError("bucketName", "Please select a bucket to add samples to");
        }
        if (CollectionUtils.isNotEmpty(selectedReworkVessels)) {
            if (reworkReason == null) {
                addValidationError("reworkReason", "A reason is required for rework vessels");
            } else {
                if (reworkReason.equals(OTHER_REASON_REFERENCE) && StringUtils.isBlank(userReworkReason)) {
                    addValidationError("reworkReason",
                            "When choosing 'Other...' for a reason, you must enter an alternate reason");
                }
            }

            if (StringUtils.isEmpty(commentText)) {
                addValidationError("commentText", "A Comment is required for rework vessels");
            }
        }
    }

    @HandlesEvent(ADD_SAMPLE_ACTION)
    public Resolution addSample() {

        String submittedReason;
        if (reworkReason.equals(OTHER_REASON_REFERENCE)) {
            submittedReason = userReworkReason.trim();
        } else {
            submittedReason = reworkReason;
        }
        if (getBuckets().isEmpty()) {
            addValidationError("vesselLabel", "{2} is not in a bucket.", vesselLabel);
        }

        for (String selectedBucketCandidate : selectedBucketCandidates) {
            ReworkEjb.BucketCandidate candidate = ReworkEjb.BucketCandidate.fromString(selectedBucketCandidate,
                    productOrderDao);
            candidate.setReworkItem(selectedReworkVessels.contains(candidate.toString()));
            bucketCandidates.add(candidate);
        }

        try {
            Collection<String> validationMessages = reworkEjb.addAndValidateCandidates(bucketCandidates,
                    submittedReason, commentText, getUserBean().getLoginUserName(), bucketName);

            if (CollectionUtils.isNotEmpty(validationMessages)) {
                for (String validationMessage : validationMessages) {
                    addGlobalValidationError(validationMessage);
                }
            } else {
                addMessage("{0} vessel(s) have been added to the {1} bucket.", bucketCandidates.size(), bucketName);
            }

        } catch (ValidationException e) {
            addGlobalValidationError(e.getMessage());
            return view();
        }

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


    @Before(stages = LifecycleStage.BindingAndValidation, on = {VESSEL_INFO_ACTION, ADD_SAMPLE_ACTION})
    public void initWorkflowBuckets() {
        Set<String> bucketNames = new HashSet<>();
        for (ProductWorkflowDef workflowDef : WorkflowLoader.getWorkflowConfig().getProductWorkflowDefs()) {
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            for (WorkflowBucketDef workflowBucketDef : workflowVersion.getBuckets()) {
                if (bucketNames.add(workflowBucketDef.getName())) {
                    buckets.add(workflowBucketDef);
                }
            }
        }
        // Set the initial bucket to the first one found.
        if (!buckets.isEmpty()) {
            bucketName = buckets.iterator().next().getName();
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = VESSEL_INFO_ACTION)
    public void setUpReworkCandidates() {
        List<String> searchTerms = SearchActionBean.cleanInputStringForSamples(vesselLabel);
        numQueryInputs = searchTerms.size();

        bucketCandidates = new ArrayList<>(reworkEjb.findBucketCandidates(searchTerms));

        Set<String> barcodes = new HashSet<>();
        Set<String> sampleIds = new HashSet<>();
        for (ReworkEjb.BucketCandidate bucketCandidate : bucketCandidates) {
            barcodes.add(bucketCandidate.getTubeBarcode());
            sampleIds.add(bucketCandidate.getSampleKey());
        }
        for (String searchTerm : searchTerms) {
            if (!barcodes.contains(searchTerm) && !sampleIds.contains(searchTerm)) {
                noResultQueryTerms.add(searchTerm);
            }
        }
    }

    @HandlesEvent(VESSEL_INFO_ACTION)
    public ForwardResolution vesselInfo() {
        if (labVessel == null) {
            addGlobalValidationError("Mercury does not recognize vessel with barcode {0}.", vesselLabel);
        }
        return new ForwardResolution(VESSEL_INFO_PAGE);
    }

    public List<ReworkReason> getAllReworkReasons() {
        return reworkReasonDao.findAll();
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

    public int getNumQueryInputs() {
        return numQueryInputs;
    }

    public void setNumQueryInputs(int numQueryInputs) {
        this.numQueryInputs = numQueryInputs;
    }

    public List<String> getNoResultQueryTerms() {
        return noResultQueryTerms;
    }

    public void setNoResultQueryTerms(List<String> noResultQueryTerms) {
        this.noResultQueryTerms = noResultQueryTerms;
    }

    public List<WorkflowBucketDef> getBuckets() {
        return buckets;
    }

    public WorkflowBucketDef getBucket() {
        return bucket;
    }

    public void setBucket(WorkflowBucketDef bucket) {
        this.bucket = bucket;
    }

    public String getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(String reworkReason) {
        this.reworkReason = reworkReason;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public List<ReworkEjb.BucketCandidate> getBucketCandidates() {
        return bucketCandidates;
    }

    public List<String> getselectedBucketCandidates() {
        return selectedBucketCandidates;
    }

    public void setselectedBucketCandidates(List<String> selectedBucketCandidates) {
        this.selectedBucketCandidates = selectedBucketCandidates;
    }

    public Set<String> getSelectedReworkVessels() {
        return selectedReworkVessels;
    }

    public void setSelectedReworkVessels(Set<String> selectedReworkVessels) {
        this.selectedReworkVessels = selectedReworkVessels;
    }

    public String getUserReworkReason() {
        return userReworkReason;
    }

    public void setUserReworkReason(String userReworkReason) {
        this.userReworkReason = userReworkReason;
    }
}
