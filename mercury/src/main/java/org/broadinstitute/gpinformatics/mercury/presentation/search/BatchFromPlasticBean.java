package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.primefaces.event.ToggleEvent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class BatchFromPlasticBean {
    @Inject
    private LabVesselDao    labVesselDao;
    @Inject
    private BSPUserList     bspUserList;
    private String          barcode;
    private LabVessel       selectedVessel;
    private List<LabVessel> foundVessels;
    private Map<LabVessel, Integer> vesselSampleSizeMap = new HashMap<LabVessel, Integer>();
    private String      jiraKey;

    public LabVessel[] getSelectedVessels() {
        return selectedVessels;
    }

    private LabVessel[] selectedVessels;

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

    public void createBatch() {

    }

    public void onRowToggle(ToggleEvent event) {
        selectedVessel = labVesselDao.findByIdentifier(((LabVessel) event.getData()).getLabel());
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(String jiraKey) {
        this.jiraKey = jiraKey;
    }

    public void setSelectedVessels(LabVessel[] selectedVessels) {
        this.selectedVessels = selectedVessels;
    }
}
