package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.*;

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
    TwoDBarcodedTubeDAO tubeDAO;

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";

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

    public String createBatch() {
        LabBatch batchObject;

        if (isUseExistingTicket()) {
            conversationData.setJiraKey(this.jiraTicketId);

            batchObject = labBatchEjb.createLabBatch(userBean.getBspUser()
                    .getUsername(), conversationData.getVesselLabels(), jiraTicketId);
        } else {

//            Collection<TwoDBarcodedTube> tubeList =
//                    tubeDAO.findByBarcodes(conversationData.getVesselLabels()).values();
//
//            Set<LabVessel> vesselSet = new HashSet<LabVessel>(tubeList);
            Set<LabVessel> vesselSet =
                    new HashSet<LabVessel>(tubeDAO.findByBarcodes(conversationData.getVesselLabels()).values());

            batchObject = new LabBatch(batchName, vesselSet);
            batchObject.setBatchDescription(batchDescription);
            batchObject.setDueDate(batchDueDate);

            labBatchEjb.createLabBatch(batchObject, userBean.getBspUser().getUsername(), null);
        }

        addInfoMessage(
                MessageFormat.format("Lab batch ''{0}'' ({1}) has been created",
                        batchObject.getBatchName(), batchObject.getJiraTicket().getTicketName())
        );

        conversationData.setBatchObject(batchObject);

//        conversationData.endConversation();
        return redirect("/search/batch_confirm");
    }
}
