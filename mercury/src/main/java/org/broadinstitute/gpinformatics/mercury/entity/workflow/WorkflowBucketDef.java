package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.bucketevaluator.BucketEntryEvaluator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where samples are placed for batching, typically the first step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowBucketDef extends WorkflowStepDef {
    private static final Log log = LogFactory.getLog(WorkflowBucketDef.class);

    private List<String> bucketEntryEvaluators=new ArrayList<>();

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
     * Set bucketEntryEvaluators; package-local access because it is used for testing.
     */
    void setBucketEntryEvaluators(List<String> bucketEntryEvaluators) {
        this.bucketEntryEvaluators = bucketEntryEvaluators;
    }

    /**
     * get bucketEntryEvaluators; package local-access because it is used for testing.
     */
    List<String> getBucketEntryEvaluators() {
        return bucketEntryEvaluators;
    }

    /**
     * Find the vessels eligible for this bucket. When bucket is being evaluated BucketEntryEvaluators defined in
     * the configuration are or-ed. If no BucketEntryEvaluators are defined it will by default allow the
     * labVessel to be bucketed.
     *
     * @return true if the vessel can go into the bucket, false otherwise
     */
    public Collection<LabVessel> meetsBucketCriteria(Collection<LabVessel> labVessels, ProductOrder productOrder) {
        if (!CollectionUtils.isNotEmpty(bucketEntryEvaluators)) {
            // If no bucketEntryEvaluators are configured, then, by default, the labVessels meet bucket criteria.
            return labVessels;
        }
        Set<LabVessel> vesselsForBucket = new HashSet<>();
        for (String bucketEntryEvaluator : bucketEntryEvaluators) {
            try {
                Class<?> bucketEntryEvaluatorClass = Class.forName(bucketEntryEvaluator.trim());
                Constructor<?> bucketEntryEvaluatorConstructor = bucketEntryEvaluatorClass.getDeclaredConstructor();
                BucketEntryEvaluator bucketEntryInstance =
                        (BucketEntryEvaluator) bucketEntryEvaluatorConstructor.newInstance();
                for (LabVessel labVessel : labVessels) {
                    if (bucketEntryInstance.invoke(labVessel, productOrder)) {
                        vesselsForBucket.add(labVessel);
                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                    InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(
                        String.format("error invoking BucketEntryEvaluator %s", bucketEntryEvaluator), e);
            }
        }
        return vesselsForBucket;
    }

    /**
     * Test if the vessel is eligible for bucketing. When bucket is being evaluated BucketEntryEvaluators defined in
     * the configuration are or-ed. If no BucketEntryEvaluators are defined it will by default allow the
     * labVessel to be bucketed.
     *
     * @return true if the vessel can go into the bucket, false otherwise
     */
    public boolean meetsBucketCriteria(LabVessel labVessel, ProductOrder productOrder) {
        return !meetsBucketCriteria(Collections.singleton(labVessel), productOrder).isEmpty();
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
}
