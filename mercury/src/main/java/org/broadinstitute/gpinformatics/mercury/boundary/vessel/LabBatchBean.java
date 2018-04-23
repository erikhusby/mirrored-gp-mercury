package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Used to create BSP batches.
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
public class LabBatchBean {
    private String batchId;
    private String workflowName;
    private List<TubeBean> tubeBeans = new ArrayList<>();
    private ParentVesselBean parentVesselBean;
    private String username;
    private Date createdDate;

    public LabBatchBean(String batchId, String workflowName, List<TubeBean> tubeBeans) {
        this.batchId = batchId;
        this.workflowName = workflowName;
        this.tubeBeans = tubeBeans;
    }

    public LabBatchBean(String batchId, ParentVesselBean parentVesselBean, String username) {
        this.batchId = batchId;
        this.parentVesselBean = parentVesselBean;
        this.username = username;
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

    public ParentVesselBean getParentVesselBean() {
        return parentVesselBean;
    }

    public void setParentVesselBean(
            ParentVesselBean parentVesselBean) {
        this.parentVesselBean = parentVesselBean;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
