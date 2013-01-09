package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jsf.TableData;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.primefaces.event.ToggleEvent;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.New;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.*;

/**
 * Backing bean for Step one of the create batch Wizard.  The main duties of this backing bean is to search for lab
 * vessels and assist in transitioning the user to the second screen to enter Jira related inforation for the Batch
 */
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


    private List<LabVessel> foundVessels;

    /*
        Maps a vessel to the number of samples registered with the vessel
     */
    private Map<LabVessel, Integer> vesselSampleSizeMap = new HashMap<LabVessel, Integer>();

    /*
        Maps a vessel to the Batches associated with the vessel
     */
    private Map<LabVessel, List<LabBatch>> batchesByVessel = new HashMap<LabVessel, List<LabBatch>>();

    private LabVessel selectedVessel;

    /*
        Stores information to be persisted across the pages within the Batch Creation Wizard.
     */
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

    public LabVessel getSelectedVessel() {
        return selectedVessel;
    }

    public void setSelectedVessel(LabVessel selectedVessel) {
        this.selectedVessel = selectedVessel;
    }

    public Map<LabVessel, List<LabBatch>> getBatchesByVessel() {
        return batchesByVessel;
    }

    public void setBatchesByVessel(Map<LabVessel, List<LabBatch>> batchesByVessel) {
        this.batchesByVessel = batchesByVessel;
    }

    public void barcodeSearch(String barcode) {
        this.barcode = barcode;
        barcodeSearch();
    }

    /**
     * Executes a search of all lab vessels that match a given list of 2d Barcodes
     */
    public void barcodeSearch() {

        String[] splitBarcodes = barcode.trim().split(",");

        List<String> barcodeList = new ArrayList<String>(splitBarcodes.length);

        for (String barcode : splitBarcodes) {
            barcodeList.add(barcode.trim());
        }

        foundVessels = labVesselDao.findByListIdentifiers(barcodeList);
        for (LabVessel foundVessel : foundVessels) {
            vesselSampleSizeMap.put(foundVessel, foundVessel.getSampleInstances().size());
            batchesByVessel.put(foundVessel, new ArrayList<LabBatch>(foundVessel.getNearestLabBatches()));
        }
    }

    public void onRowToggle(ToggleEvent event) {
        selectedVessel = labVesselDao.findByIdentifier(((LabVessel) event.getData()).getLabel());
    }

    /**
     * Helper method to retrieve the name of a user based on that users unique BspID
     *
     * @param id
     * @return
     */
    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
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

    /**
     * Form initializer for the first screen in the Wizard.  The main duty of this initializer is to start the
     * conversation that will store the wizard data.
     */
    public void initForm() {
        if (userBean.ensureUserValid()) {
            if (conversationData.getConversation().isTransient()) {
                conversationData.beginConversation();
            }
        } else {
            addErrorMessage(MessageFormat.format(UserBean.LOGIN_WARNING,
                    "Create a Batch"));
        }
    }
}
