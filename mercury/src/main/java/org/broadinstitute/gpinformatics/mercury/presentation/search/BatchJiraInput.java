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
 * Backing Bean for the second page in the Batch input.  This been bears the responsibility of assisting in processing
 * the Jira related data for the newly created batch
 *
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

    /*
        Helper constants for the selection radio buttons on the screen
     */
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

    /**
     * Event Toggle for every time a user choose a Jira input method
     * <p/>
     * TODO SGM:  This may be one extra method that is not needed.  Revisit how to consolidate input type check after demo
     *
     * @param event
     */
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

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return
     */
    public String createBatch() {
        LabBatch batchObject;

        Set<LabVessel> vesselSet =
                new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(conversationData.getVesselLabels()));

        if (isUseExistingTicket()) {
            /*
               If the user is associating the batch with an existing ticket, just the ticket ID and the set of vessels
               are needed to create the batch
            */

            batchObject =
                    labBatchEjb.createLabBatch(vesselSet, userBean.getBspUser().getUsername(),
                            jiraTicketId.trim());
        } else {

            /*
                If a new ticket is to be created, pass the description, summary, due date and important info in a batch
                object acting as a DTO
             */
            batchObject = new LabBatch(batchName.trim(), vesselSet, batchDescription, batchDueDate, batchImportantInfo);

            labBatchEjb.createLabBatch(batchObject, userBean.getBspUser().getUsername());
        }

        addInfoMessage(
                MessageFormat.format("Lab batch ''{0}'' has been created", batchObject.getJiraTicket().getTicketName())
        );

        conversationData.setBatchObject(batchObject);

        conversationData.endConversation();
        return redirect("/search/batch_confirm", "labBatch=" + batchObject.getBatchName());
    }

}
