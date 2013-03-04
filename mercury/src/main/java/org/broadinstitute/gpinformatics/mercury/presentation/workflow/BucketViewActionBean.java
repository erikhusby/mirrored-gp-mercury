package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.CreateBatchActionBean;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(value = "/view/bucketView.action")
public class BucketViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/workflow/bucketView.jsp";
    @Inject
    private LabEventHandler labEventHandler;
    @Inject
    private WorkflowLoader workflowLoader;
    @Inject
    private BucketDao bucketDao;
    @Inject
    private AthenaClientService athenaClientService;

    private List<WorkflowBucketDef> buckets = new ArrayList<WorkflowBucketDef>();
    @Validate(required = true, on = "viewBucket")
    private String selectedBucket;

    private Collection<BucketEntry> bucketEntries;

    private Map<String, ProductOrder> pdoByKeyMap = new HashMap<String, ProductOrder>();

    private boolean jiraEnabled = false;

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        WorkflowConfig workflowConfig = workflowLoader.load();
        List<ProductWorkflowDef> workflowDefs = workflowConfig.getProductWorkflowDefs();
        //currently only do ExEx
        for (ProductWorkflowDef workflowDef : workflowDefs) {
            ProductWorkflowDefVersion workflowVersion = workflowDef.getEffectiveVersion();
            if (workflowDef.getName().equals(WorkflowName.EXOME_EXPRESS.getWorkflowName())) {
                buckets.addAll(workflowVersion.getBuckets());
            }
        }
    }

    public List<WorkflowBucketDef> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<WorkflowBucketDef> buckets) {
        this.buckets = buckets;
    }

    public String getSelectedBucket() {
        return selectedBucket;
    }

    public void setSelectedBucket(String selectedBucket) {
        this.selectedBucket = selectedBucket;
    }

    public Collection<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    public void setBucketEntries(Collection<BucketEntry> bucketEntries) {
        this.bucketEntries = bucketEntries;
    }

    public boolean isJiraEnabled() {
        return jiraEnabled;
    }

    public void setJiraEnabled(boolean jiraEnabled) {
        this.jiraEnabled = jiraEnabled;
    }

    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    public Resolution viewBucket() {
        if (selectedBucket != null) {
            Bucket bucket = bucketDao.findByName(selectedBucket);
            if (bucket != null) {
                bucketEntries = bucket.getBucketEntries();
            } else {
                bucketEntries = new ArrayList<BucketEntry>();
            }
            if (bucketEntries.size() > 0) {
                jiraEnabled = true;
                for(BucketEntry bucketEntry:bucketEntries) {
                    pdoByKeyMap.put(bucketEntry.getPoBusinessKey(),
                                    athenaClientService.retrieveProductOrderDetails(bucketEntry.getPoBusinessKey()));
                }
            }
        }
        return view();
    }

    public ProductOrder getPDODetails(String pdoKey) {
        if(!pdoByKeyMap.containsKey(pdoKey)) {
            pdoByKeyMap.put(pdoKey, athenaClientService.retrieveProductOrderDetails(pdoKey));
        }
        return pdoByKeyMap.get(pdoKey);
    }
}
