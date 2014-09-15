package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

//import org.mvel2.MVEL;
//import org.mvel2.optimizers.OptimizerFactory;

/**
 * Where samples are placed for batching, typically the first step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowBucketDef extends WorkflowStepDef {

    /** Expression to determine whether a vessel can enter the bucket */
    private String entryExpression;

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

    public boolean meetsBucketCriteria(LabVessel labVessel) {
        // Samples with a Mercury metadata source always meet the criteria, because they don't have material types.
        for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
            for (MercurySample mercurySample : sampleInstanceV2.getRootMercurySamples()) {
                if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    return true;
                }
            }
        }

        // todo remove this code block when Bamboo works with MVEL
        if (entryExpression != null && entryExpression.contains("getMaterialType() contains \"DNA:\"")) {
            return labVessel.isDNA();
        }

        // Compile, even though we're using it only once, because MVEL sometimes has
        // problems with Hibernate proxies in eval method
        // todo uncomment this code block when Bamboo works with MVEL
//        OptimizerFactory.setDefaultOptimizer("reflective");
//        Serializable compiled = MVEL.compileExpression(entryExpression);
//        Map<String, Object> context = new HashMap<String, Object>();
//        context.put("labVessel", labVessel);
//        return (Boolean) MVEL.executeExpression(compiled, context);

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
