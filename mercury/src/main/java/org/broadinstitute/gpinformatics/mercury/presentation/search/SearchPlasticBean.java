package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.primefaces.event.ToggleEvent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.util.*;

@ManagedBean
@ViewScoped
public class SearchPlasticBean {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private BSPUserList bspUserList;
    private String barcode;
    private LabVessel selectedVessel;
    private List<LabVessel> foundVessels;
    private Map<LabVessel, Integer> vesselSampleSizeMap = new HashMap<LabVessel, Integer>();
    private Map<LabVessel, List<LabBatch>> batchesByVessel = new HashMap<LabVessel, List<LabBatch>>();


    public Map<LabVessel, Integer> getVesselSampleSizeMap() {
        return vesselSampleSizeMap;
    }

    public Map<LabVessel, List<LabBatch>> getBatchesByVessel() {
        return batchesByVessel;
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
            batchesByVessel.put(foundVessel, new ArrayList<LabBatch>(foundVessel.getNearestLabBatches()));
        }
    }

    public void onRowToggle(ToggleEvent event) {
        selectedVessel = labVesselDao.findByIdentifier(((LabVessel) event.getData()).getLabel());
    }

    public String getUserNameById(Long id){
        BspUser user = bspUserList.getById(id);
        String username = "";
        if(user != null){
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }
}
