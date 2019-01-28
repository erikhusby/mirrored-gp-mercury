package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A step in a process
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowStepDef implements Serializable {

    private static final long serialVersionUID = 20130101L;

    enum QuantType {
        PICO,
        ECO_QPCR,
        FINAL_LIBRARY_SIZE,
        AGILENT,
        RIBO
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

    enum EventClass {
        IN_PLACE,
        TRANSFER
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
    private List<LabEventType> labEventTypes = new ArrayList<>();
    private EventClass eventClass;
    /** Specific reagent types for the generic ADD_REAGENT event. */
    private List<String> reagentTypes = new ArrayList<>();
    /** Specific vessel type for the generic ADD_REAGENT event.  JAXB doesn't allow use of VesselTypeGeometry here. */
    private BarcodedTube.BarcodedTubeType targetBarcodedTubeType;
    /** Whether this step is optional, e.g. normalization is optional if the concentration is fine as is */
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
    /** Identifies if this step could create a Jira ticket, and the type of ticket that should be created */
    private String batchJiraProjectType;
    /** The category of output, e.g. library */
    private OutputCategory outputCategory;
    /** The type of output, e.g. enriched */
    private OutputType outputType;
    /** How many times the message is repeated, e.g. a transfer to duplicate Pico plates has an original transfer, and one repeat */
    private Integer numberOfRepeats = 0;
    /** Determines whether the current workflow step is on a path that does not lead towards the ned of the current
     * process */
    private boolean deadEndBranch = false;
    /** Qualifies generic events like CENTRIFUGE, which might occur multiple times in one process. */
    private String workflowQualifier;
    /** Instructions to the users. */
    private String instructions;
    /** Lab Batch Workflow type to assist with auto batch selection **/
    private String batchJiraIssueType;
    /** Configuration for the Manual Transfers page. */
    private LabEventType.ManualTransferDetails manualTransferDetails;
    private List<JiraTransitionType> jiraTransition = new ArrayList<>();

    private boolean ancestryEtlFlag = false;

    private transient WorkflowProcessDef processDef;

    private transient WorkflowProcessDefVersion processDefVersion;


    /** For JAXB */
    WorkflowStepDef() {
    }

    public void setProcessDef(WorkflowProcessDef processDef) {
        this.processDef = processDef;
    }

    public void setBatchJiraIssueType(String issueType) {
        this.batchJiraIssueType = issueType;
    }

    public void setBatchJiraProjectType(String issueType) {
        this.batchJiraProjectType = issueType;
    }

    public WorkflowProcessDef getProcessDef() {
        return processDef;
    }

    public WorkflowStepDef(String name) {
        this.name = name;
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

    public EventClass getEventClass() {
        return eventClass;
    }

    public List<String> getReagentTypes() {
        return reagentTypes;
    }

    public BarcodedTube.BarcodedTubeType getTargetBarcodedTubeType() {
        return targetBarcodedTubeType;
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

    public WorkflowProcessDefVersion getProcessDefVersion () {
        return processDefVersion;
    }

    public String getBatchJiraProjectType() {
        return batchJiraProjectType;
    }

    public void setProcessDefVersion ( WorkflowProcessDefVersion processDefVersion ) {
        this.processDefVersion = processDefVersion;
    }

    public Integer getNumberOfRepeats() {
        return numberOfRepeats;
    }

    public boolean isDeadEndBranch() {
        return deadEndBranch;
    }

    public boolean doAncestryEtl(){
        return ancestryEtlFlag;
    }

    public String getWorkflowQualifier() {
        return workflowQualifier;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getBatchJiraIssueType() {
        return batchJiraIssueType;
    }

    public LabEventType.ManualTransferDetails getManualTransferDetails() {
        return manualTransferDetails;
    }

    public List<JiraTransitionType> getJiraTransition() {
        return jiraTransition;
    }

    public void setJiraTransition(
            List<JiraTransitionType> jiraTransition) {
        this.jiraTransition = jiraTransition;
    }
}
