package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Represents the flattened workflowConfig record, used for ETL of WorkflowConfig.
 */
public class WorkflowConfigDenorm {
    private static final Log logger = LogFactory.getLog(WorkflowConfigDenorm.class);

    private final long workflowConfigDenormId;
    private final long workflowId;
    private final long processId;
    private final Date effectiveDate;
    private final String productWorkflowName;
    private final String productWorkflowVersion;
    private final String workflowProcessName;
    private final String workflowProcessVersion;
    private final String workflowStepName;
    private final String workflowStepEventName;


    public WorkflowConfigDenorm(Date effectiveDate,
                                String productWorkflowName,
                                String productWorkflowVersion,
                                String workflowProcessName,
                                String workflowProcessVersion,
                                String workflowStepName,
                                String workflowStepEventName) {

        this.effectiveDate = effectiveDate;
        this.productWorkflowName = productWorkflowName;
        this.productWorkflowVersion = productWorkflowVersion;
        this.workflowProcessName = workflowProcessName;
        this.workflowProcessVersion = workflowProcessVersion;
        this.workflowStepName = workflowStepName;
        this.workflowStepEventName = workflowStepEventName;

        workflowConfigDenormId = calculateId(effectiveDate, productWorkflowName, productWorkflowVersion,
                workflowProcessName, workflowProcessVersion, workflowStepName, workflowStepEventName);

        workflowId = calculateWorkflowId(productWorkflowName, productWorkflowVersion);
        processId = calculateProcessId(workflowProcessName, workflowProcessVersion, workflowStepName, workflowStepEventName);
    }

    public long calculateProcessId(String processName, String version, String stepName, String eventName) {
        return GenericEntityEtl.hash(processName, version, stepName, eventName);
    }

    public long calculateWorkflowId(String productWorkflowName, String productWorkflowVersion) {
        return GenericEntityEtl.hash(productWorkflowName, productWorkflowVersion);
    }

    public long getWorkflowConfigDenormId() {
        return workflowConfigDenormId;
    }

    public long getWorkflowId() {
        return workflowId;
    }

    public long getProcessId() {
        return processId;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public String getEffectiveDateStr() {
        return ExtractTransform.secTimestampFormat.format(effectiveDate);
    }

    public String getProductWorkflowName() {
        return productWorkflowName;
    }

    public String getProductWorkflowVersion() {
        return productWorkflowVersion;
    }

    public String getWorkflowProcessName() {
        return workflowProcessName;
    }

    public String getWorkflowProcessVersion() {
        return workflowProcessVersion;
    }

    public String getWorkflowStepName() {
        return workflowStepName;
    }

    public String getWorkflowStepEventName() {
        return workflowStepEventName;
    }

    /**
     * Calculates a workflowConfigDenormId using a deterministic algorithm.
     */
    public long calculateId(Date effectiveDate, String productWorkflowName, String productWorkflowVersion, String workflowProcessName,
                            String workflowProcessVersion, String workflowStepName, String workflowStepEventName) {
        return GenericEntityEtl.hash(String.valueOf(effectiveDate.getTime()), productWorkflowName, productWorkflowVersion,
                workflowProcessName, workflowProcessVersion, workflowStepName, workflowStepEventName);
    }

    /**
     * Utility method that parses the hierarchical workflowConfig and returns a
     * flattened representation used by ETL.
     *
     * @param workflowConfig the config to parse
     * @return list of denormalized config objects
     */
    public static Collection<WorkflowConfigDenorm> parse(WorkflowConfig workflowConfig) {
        Collection<WorkflowConfigDenorm> list = new ArrayList<WorkflowConfigDenorm>();
        if (workflowConfig == null) {
            return list;
        }

        for (ProductWorkflowDef pwd : workflowConfig.getProductWorkflowDefs()) {
            String productWorkflowName = pwd.getName();
            Date pwdvEndDate = null;

            for (ProductWorkflowDefVersion pwdv : pwd.getWorkflowVersionsDescEffDate()) {
                String productWorkflowVersion = pwdv.getVersion();
                Date pwdvStartDate = pwdv.getEffectiveDate();

                for (WorkflowProcessDef wf : pwdv.getWorkflowProcessDefs()) {
                    String workflowProcessName = wf.getName();

                    for (WorkflowProcessDefVersion wpdv : wf.getProcessVersionsDescEffDate()) {
                        // Net effective date is the later of the two start dates, capped by end date.
                        // Records having inconsistent date (wpdv start after pwdv end) are not kept.
                        Date wpdvStartDate = wpdv.getEffectiveDate();

                        if (pwdvEndDate != null && wpdvStartDate.after(pwdvEndDate)) {
                            logger.debug("Useless workflow config element: ProductWorkflowName "
                                    + productWorkflowName + " version " + productWorkflowVersion + " starts (" +
                                    ExtractTransform.secTimestampFormat.format(wpdvStartDate) + ") after WorkflowProcessDef "
                                    + workflowProcessName + " version " + wpdv.getVersion() + " ends (" +
                                    ExtractTransform.secTimestampFormat.format(pwdvEndDate) + ")");
                            continue;
                        }
                        Date netEffectiveDate = wpdvStartDate.after(pwdvStartDate) ? wpdvStartDate : pwdvStartDate;
                        String workflowProcessVersion = wpdv.getVersion();

                        for (WorkflowStepDef wsd : wpdv.getWorkflowStepDefs()) {
                            String workflowStepName = wsd.getName();

                            for (LabEventType eventType : wsd.getLabEventTypes()) {
                                String workflowStepEventName = eventType.getName();

                                list.add(new WorkflowConfigDenorm(netEffectiveDate, productWorkflowName,
                                        productWorkflowVersion, workflowProcessName, workflowProcessVersion,
                                        workflowStepName, workflowStepEventName));
                            }
                        }
                    }
                }
                // End date is obtained by keeping previous pwdv start date, since iteration is on date desc.
                pwdvEndDate = pwdvStartDate;
            }
        }
        return list;
    }
}

