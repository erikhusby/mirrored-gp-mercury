package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.ConversationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class BatchFromPlasticBean extends AbstractJsfBean {

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    private String barcode;
    //    private LabVessel       selectedVessel;
    private List<LabVessel> foundVessels;
    private Map<LabVessel, Integer> vesselSampleSizeMap = new HashMap<LabVessel, Integer>();

    @Inject
    private CreateBatchConversationData conversationData;

    public Map<LabVessel, Integer> getVesselSampleSizeMap() {
        return vesselSampleSizeMap;
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

/*
    public LabVessel getSelectedVessel() {
        return selectedVessel;
    }

    public void setSelectedVessel(LabVessel selectedVessel) {
        this.selectedVessel = selectedVessel;
    }
*/

    public void barcodeSearch(String barcode) {
        this.barcode = barcode;
        barcodeSearch();
    }

    public void barcodeSearch() {
        List<String> barcodeList = Arrays.asList(barcode.trim().split(","));
        foundVessels = labVesselDao.findByListIdentifiers(barcodeList);
        for (LabVessel foundVessel : foundVessels) {
            vesselSampleSizeMap.put(foundVessel, foundVessel.getSampleInstances().size());
        }
    }

    //    public void onRowToggle(ToggleEvent event) {
//        selectedVessel = labVesselDao.findByIdentifier(((LabVessel) event.getData()).getLabel());
//    }
//
    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public String getJiraKey() {
        return conversationData.getJiraKey();
    }

    public void setJiraKey(String jiraKey) {
        conversationData.setJiraKey(jiraKey);
    }

    public void setSelectedVessels(LabVessel[] selectedVessels) {
        conversationData.setSelectedVessels(selectedVessels);
    }

    public LabVessel[] getSelectedVessels() {
        return conversationData.getSelectedVessels();
    }

    public CreateBatchConversationData getConversationData() {
        return conversationData;
    }

    public void setConversationData(CreateBatchConversationData conversationData) {
        this.conversationData = conversationData;
    }

    public void initForm() {
        if (userBean.ensureUserValid()) {
            if(conversationData.getConversation().isTransient()) {
                conversationData.beginConversation();
            }
        } else {
            addErrorMessage(MessageFormat.format(UserBean.LOGIN_WARNING,
                    "create a research project"));
        }
    }
}
