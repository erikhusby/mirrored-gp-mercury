package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Inject
    private LabVesselDao labVesselDao;

    private static final String EXISTING_TICKET = "existingTicket";
    private static final String NEW_TICKET = "newTicket";

    @Inject
    private CreateBatchConversationData conversationData;

    @Inject
    private UserBean userBean;


    private String jiraInputType = EXISTING_TICKET;
    private String jiraTicketId = "";
    private String batchName = "";
    private String batchDescription = "";
    private Date batchDueDate;

    private boolean useExistingTicket = true;
    private String batchImportantInfo;

    public void setJiraInputType(String jiraInputType) {
        this.jiraInputType = jiraInputType;
    }

    public void updateTicketUsage(AjaxBehaviorEvent event) {
        this.useExistingTicket = (jiraInputType == null) ? false : jiraInputType.equals(EXISTING_TICKET);
    }

    public String getJiraInputType() {
        return jiraInputType;
    }

    public boolean isUseExistingTicket() {
        return useExistingTicket;
    }

    public void setUseExistingTicket(boolean useExistingTicket) {
        this.useExistingTicket = useExistingTicket;
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

    public CreateBatchConversationData getConversationData() {
        return conversationData;
    }

    public void setBatchImportantInfo(String batchImportantInfo) {
        this.batchImportantInfo = batchImportantInfo;
    }

    public String getBatchImportantInfo() {
        return batchImportantInfo;
    }

    public String createBatch() {
        LabBatch batchObject;

        Set<LabVessel> vesselSet =
                new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(conversationData.getVesselLabels()));

        if (isUseExistingTicket()) {

            batchObject =
                    labBatchEjb.createLabBatch(vesselSet,userBean.getBspUser().getUsername(),
                            jiraTicketId.trim());
        } else {

            batchObject = new LabBatch(batchName.trim(), vesselSet);
            batchObject.setBatchDescription(batchDescription.trim());
            batchObject.setDueDate(batchDueDate);
            batchObject.setImportant(batchImportantInfo);

            labBatchEjb.createLabBatch(batchObject, userBean.getBspUser().getUsername());
        }

        addInfoMessage(
                MessageFormat.format("Lab batch ''{0}'' has been created", batchObject.getJiraTicket().getTicketName())
        );

        conversationData.setBatchObject(batchObject);

        conversationData.endConversation();
        return redirect("/search/batch_confirm","labBatch="+batchObject.getBatchName());
    }

}
