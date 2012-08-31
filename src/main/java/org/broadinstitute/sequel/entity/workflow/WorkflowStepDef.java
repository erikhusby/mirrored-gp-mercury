package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.labevent.LabEventType;

import java.util.List;

/**
 * A step in a process
 */
public class WorkflowStepDef {

    enum QuantType {
        PICO,
        ECO_QPCR,
        FINAL_LIBRARY_SIZE,
        AGILENT
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
    private List<LabEventType> labEventTypes;
    private boolean optional;
    /** decision, perhaps expressed in MVEL */
    private String checkpointExpression;
    /** entry point - support adding sample metadata for walk up sequencing */
    private boolean entryPoint;
    /** re-entry point - notice to batch watchers */
    private boolean reEntryPoint;
    /** QC point - data uploaded */
    private QuantType quantType;

    public WorkflowStepDef(String name) {
        this.name = name;
    }

    public WorkflowStepDef addLabEvent(LabEventType labEventType) {
        labEventTypes.add(labEventType);
        return this;
    }
}
