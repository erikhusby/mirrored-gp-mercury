package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
     * Test if the vessel is eligible for bucketing. When bucket is being evaluated BucketEntryEvaluators defined in
     * the configuration are and-ed together. If no BucketEntryEvaluators are defined it will by default allow the
     * labVessel to be bucketed.
     *
     * @return true if the vessel can go into the bucket, false otherwise
     */
    public boolean meetsBucketCriteria(LabVessel labVessel) {
        if (CollectionUtils.isNotEmpty(bucketEntryEvaluators)) {
            for (String bucketEntryEvaluator : bucketEntryEvaluators) {
                try {
                    Class<?> bucketEntryEvaluatorClass = Class.forName(bucketEntryEvaluator);
                    Constructor<?> bucketEntryEvaluatorConstructor = bucketEntryEvaluatorClass.getDeclaredConstructor();
                    BucketEntryEvaluator bucketEntryInstance =
                            (BucketEntryEvaluator) bucketEntryEvaluatorConstructor.newInstance();
                    boolean meetsCriteria = bucketEntryInstance.invoke(labVessel);
                    if (!meetsCriteria) {
                        return false;
                    }
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                        InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(
                            String.format("error invoking BucketEntryEvaluator %s", bucketEntryEvaluator), e);
                }
            }
        }
        return true;
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
