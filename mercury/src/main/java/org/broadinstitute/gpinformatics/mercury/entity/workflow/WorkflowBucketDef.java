package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Where samples are placed for batching, typically the first step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowBucketDef extends WorkflowStepDef {
    private static final Log log = LogFactory.getLog(WorkflowBucketDef.class);

    private WorkflowBucketEntryEvaluator bucketEntryEvaluator;
    private Boolean autoBucketFromPdoSubmission;
    private Boolean jiraSampleFromNearest;

    /** auto-drain rules - time / date based */
    private Double autoDrainDays;

    /** For JAXB */
    WorkflowBucketDef() {
    }

    public WorkflowBucketDef(String name) {
        super(name);
    }

    public Double getAutoDrainDays() {
        return autoDrainDays;
    }

    public void setAutoDrainDays(Double autoDrainDays) {
        this.autoDrainDays = autoDrainDays;
    }

    /**
     * Test if the vessel is eligible for bucketing. When bucket is being evaluated BucketEntryEvaluators defined in
     * the configuration are or-ed. If no BucketEntryEvaluators are defined it will by default allow the
     * labVessel to be bucketed.
     *
     * @return true if the vessel can go into the bucket, false otherwise
     */
    public boolean meetsBucketCriteria(LabVessel labVessel, ProductOrder productOrder) {
        if (bucketEntryEvaluator == null) {
            // If no bucketEntryEvaluators are configured, then, by default, the labVessels meet bucket criteria.
            return true;
        }

        return bucketEntryEvaluator.invoke(labVessel, productOrder);
    }

    public String getWorkflowForProductOrder(ProductOrder productOrder) {
        String workflow = bucketEntryEvaluator.getMatchingWorkflow(productOrder);
        if (workflow == null) {
            workflow = productOrder.getProduct().getWorkflowName();
        }
        return workflow;
    }

    /**
     * Returns the type of event to be created when on a vessel entering the bucket.
     *
     * @return the bucket entry event type
     */
    public LabEventType getBucketEventType() {
        if (!getLabEventTypes().isEmpty()) {
            return getLabEventTypes().get(0);
        } else {
            return null;
        }
    }

    void setBucketEntryEvaluator(WorkflowBucketEntryEvaluator bucketEntryEvaluator) {
        this.bucketEntryEvaluator = bucketEntryEvaluator;
    }

    WorkflowBucketEntryEvaluator getBucketEntryEvaluator() {
        return bucketEntryEvaluator;
    }

    public String findMissingRequirements(ProductOrder productOrder, MaterialType latestMaterialType) {
        return bucketEntryEvaluator.findMissingRequirements(productOrder, latestMaterialType);
    }

    public boolean isAutoBucketFromPdoSubmission() {
        return autoBucketFromPdoSubmission == null ? true : autoBucketFromPdoSubmission;
    }

    public boolean isJiraSampleFromNearest() {
        return jiraSampleFromNearest == null ? true : jiraSampleFromNearest;
    }
}
