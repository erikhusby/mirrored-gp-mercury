package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import com.sun.tools.javac.resources.version;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
//import org.mvel2.MVEL;
//import org.mvel2.optimizers.OptimizerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

        // todo remove this code block when Bamboo works with MVEL
        if (entryExpression != null && entryExpression.contains("getMaterialType() == \"DNA:DNA Genomic\"")) {
            for (SampleInstance si : labVessel.getSampleInstances()) {
                if (si.getStartingSample().getBspSampleDTO().getMaterialType() == "DNA:DNA Genomic") {
                    return true;
                }
            }
            return false;
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

}
