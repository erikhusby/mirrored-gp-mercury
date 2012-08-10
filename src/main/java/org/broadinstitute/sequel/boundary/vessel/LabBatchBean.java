package org.broadinstitute.sequel.boundary.vessel;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to import racks from Squid
 */
@XmlRootElement
public class LabBatchBean {
    private String batchId;
    private String workflowName;
    private List<TubeBean> tubeBeans = new ArrayList<TubeBean>();

    public LabBatchBean(String batchId, String workflowName, List<TubeBean> tubeBeans) {
        this.batchId = batchId;
        this.workflowName = workflowName;
        this.tubeBeans = tubeBeans;
    }

    /** Used by JAXB */
    @SuppressWarnings("UnusedDeclaration")
    public LabBatchBean() {
    }

    public String getBatchId() {
        return batchId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public List<TubeBean> getTubeBeans() {
        return tubeBeans;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setTubeBeans(List<TubeBean> tubeBeans) {
        this.tubeBeans = tubeBeans;
    }
}
