package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.glassfish.gmbal.ManagedData;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

/**
 * @author Scott Matthews
 *         Date: 12/12/12
 *         Time: 10:46 AM
 */
@ManagedBean
@ViewScoped
public class BatchJiraInput extends AbstractJsfBean {

    @Inject
    private LabBatchEjb labBatchEjb;

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";

    @Inject
    private CreateBatchConversationData conversationData;

    @Inject
    private UserBean userBean;

    private String jiraInputType = "";
    private String jiraTicketId = "";
    private String batchName = "";
    private String batchDescription = "";
    private Date batchDueDate;

    public void setJiraInputType(String jiraInputType) {
        this.jiraInputType = jiraInputType;
    }

    public String getJiraInputType() {
        return jiraInputType;
    }

    public boolean useExistingTicket() {
        return (jiraInputType == null)?false: jiraInputType.equals(EXISTING_TICKET);
    }

    public boolean makeNewJiraTicket() {
        return !useExistingTicket();
    }

    public void setJiraTicketId(String jiraTicketId) {
        this.jiraTicketId = jiraTicketId;
    }

    public String getJiraTicketId() {
        return jiraTicketId;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchDescription(String batchDescription) {
        this.batchDescription = batchDescription;
    }

    public String getBatchDescription() {
        return batchDescription;
    }

    public void setBatchDueDate(Date batchDueDate) {
        this.batchDueDate = batchDueDate;
    }

    public Date getBatchDueDate() {
        return batchDueDate;
    }

    public String createBatch() {
        if (useExistingTicket()) {
            conversationData.setJiraKey(this.jiraTicketId);

            labBatchEjb.createLabBatch(Arrays.asList(conversationData.getSelectedVessels()), userBean.getBspUser()
                    .getUsername(), jiraTicketId);
        } else {
            LabBatch batchObject =
                    new LabBatch(batchName,
                            new HashSet<LabVessel>(Arrays.asList(conversationData.getSelectedVessels())));
            batchObject.setBatchDescription(batchDescription);
            batchObject.setDueDate(batchDueDate);
            conversationData.setBatchObject(batchObject);

            labBatchEjb.createLabBatch(batchObject, userBean.getBspUser().getUsername(),null);
        }

        conversationData.endConversation();
        return redirect("/search/batch_confirm");
    }
}
