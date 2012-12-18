package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 12/17/12
 *         Time: 4:51 PM
 */
@ManagedBean
@ViewScoped
public class BatchConfirm extends AbstractJsfBean {

    @Inject
    private CreateBatchConversationData conversationData;

    @Inject
    LabBatchDAO labBatchDAO;

    @Inject
    private BSPUserList bspUserList;

    private LabBatch foundBatch;

    private List<LabVessel> listOfVessels;

    public void initForm() {

        foundBatch = labBatchDAO.findByName(conversationData.getBatchObject().getBatchName());

        listOfVessels = new ArrayList<LabVessel>(foundBatch.getStartingLabVessels());

//        if(!conversationData.getConversation().isTransient()) {
//            conversationData.endConversation();
//        }

    }

    public List<LabVessel> getListOfVessels() {
        return listOfVessels;
    }

    public void setListOfVessels(List<LabVessel> listOfVessels) {
        this.listOfVessels = listOfVessels;
    }

    public CreateBatchConversationData getConversationData() {
        return conversationData;
    }

    public void setConversationData(CreateBatchConversationData conversationData) {
        this.conversationData = conversationData;
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public LabBatch getFoundBatch() {
        return foundBatch;
    }

    public void setFoundBatch(LabBatch foundBatch) {
        this.foundBatch = foundBatch;
    }
}
