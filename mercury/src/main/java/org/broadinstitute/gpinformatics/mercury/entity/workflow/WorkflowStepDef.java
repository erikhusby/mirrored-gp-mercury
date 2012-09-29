package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
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
    private boolean optional;
    /** decision, perhaps expressed in MVEL */
    private String checkpointExpression;
    /** entry point - support adding sample metadata for walk up sequencing */
    private boolean entryPoint;
    /** re-entry point - notice to batch watchers */
    private boolean reEntryPoint;
    /** QC point - data uploaded */
    private QuantType quantType;
    private Integer expectedCycleTimeMinutes;

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
