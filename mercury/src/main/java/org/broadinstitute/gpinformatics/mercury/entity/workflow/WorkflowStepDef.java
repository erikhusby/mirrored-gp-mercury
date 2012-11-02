package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowStepDef {

    enum QuantType {
        PICO,
        ECO_QPCR,
        FINAL_LIBRARY_SIZE,
        AGILENT
    }

    enum OutputCategory {
        LIBRARY(Arrays.asList(
                OutputType.SIXTEENS_PRODUCT_POOL,
                OutputType.CONTROL_LIBRARY,
                OutputType.DENATURED_LIBRARY,
                OutputType.ENRICHED_LIBRARY,
                OutputType.EXTERNAL_LIBRARY,
                OutputType.NORMALIZED_LIBRARY,
                OutputType.PCR_PRODUCT_POOL,
                OutputType.POOLED_NORMALIZED_LIBRARY,
                OutputType.SIZE_SELECTED_ENRICHED_LIBRARY,
                OutputType.TSCA_LIBRARY,
                OutputType.UNENRICHED_LIBRARY,
                OutputType.CDNA_ENRICHED_LIBRARY
        )),
        GDNA(Arrays.asList(
                OutputType.POST_SPRI_GDNA,
                OutputType.PRENORMALIZED_GDNA,
                OutputType.NORMALIZED_GDNA
        ));

        private List<OutputType> outputTypes;

        OutputCategory(List<OutputType> outputTypes) {
            this.outputTypes = outputTypes;
        }

        public List<OutputType> getOutputTypes() {
            return outputTypes;
        }
    }

    enum OutputType {
        //LIBRARY
        SIXTEENS_PRODUCT_POOL,
        CONTROL_LIBRARY,
        DENATURED_LIBRARY,
        ENRICHED_LIBRARY,
        EXTERNAL_LIBRARY,
        NORMALIZED_LIBRARY,
        PCR_PRODUCT_POOL,
        POOLED_NORMALIZED_LIBRARY,
        SIZE_SELECTED_ENRICHED_LIBRARY,
        TSCA_LIBRARY,
        UNENRICHED_LIBRARY,
        CDNA_ENRICHED_LIBRARY,
        // GDNA
        POST_SPRI_GDNA,
        PRENORMALIZED_GDNA,
        NORMALIZED_GDNA,
    }

    /* LibraryQuantTypes:
    GSSR/BSP Pico
    Pre Flight Pre Norm Pico
    Pre Flight Post Norm Pico
    Pond Pico
    Catch Pico
    Post-Normalization Pico
    TSCA Pico
    Final Library Size*/

    private String name;
    private List<LabEventType> labEventTypes = new ArrayList<LabEventType>();
    /** Whether this step is optional, e.g. normalization is otional if the concentration is fine as is */
    private boolean optional;
    /** decision, perhaps expressed in MVEL */
    private String checkpointExpression;
    /** entry point - support adding sample metadata for walk up sequencing */
    private boolean entryPoint;
    /** re-entry point - notice to batch watchers */
    private boolean reEntryPoint;
    /** QC point - data uploaded */
    private QuantType quantType;
    /** How long we expect to spend in this step */
    private Integer expectedCycleTimeMinutes;
    /** The category of output, e.g. library */
    private OutputCategory outputCategory;
    /** The type of output, e.g. enriched */
    private OutputType outputType;

    public WorkflowStepDef(String name) {
        this.name = name;
    }

    /** For JAXB */
    WorkflowStepDef() {
    }

    public WorkflowStepDef addLabEvent(LabEventType labEventType) {
        labEventTypes.add(labEventType);
        return this;
    }

    public String getName() {
        return name;
    }

    public List<LabEventType> getLabEventTypes() {
        return labEventTypes;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getCheckpointExpression() {
        return checkpointExpression;
    }

    public boolean isEntryPoint() {
        return entryPoint;
    }

    public boolean isReEntryPoint() {
        return reEntryPoint;
    }

    public QuantType getQuantType() {
        return quantType;
    }

    public Integer getExpectedCycleTimeMinutes() {
        return expectedCycleTimeMinutes;
    }
}
