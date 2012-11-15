package org.broadinstitute.gpinformatics.mercury.presentation.lookup;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.primefaces.event.ToggleEvent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean(name = "lookupBean")
@ViewScoped
public class LookupBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;
    private String barcode;
    private LabVessel selectedVessel;
    private List<LabVessel> foundVessels;
    private Map<LabVessel, Integer> vesselSampleSizeMap = new HashMap<LabVessel, Integer>();

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

    public void onRowToggle(ToggleEvent event) {
        selectedVessel = labVesselDao.findByIdentifier(((LabVessel) event.getData()).getLabel());
    }
}
