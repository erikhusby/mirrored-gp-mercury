package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ManagedBean
@ViewScoped
public class SearchPlasticBean {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private BSPUserList bspUserList;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private ProductOrderDao productOrderDao;

    private String searchKey;
    private LabVessel selectedVessel;
    private List<LabVessel> foundVessels;
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private Boolean showPlateLayout = false;
    private Boolean showSampleList = false;

    private Map<LabVessel, Integer> vesselSampleSizeMap = new HashMap<LabVessel, Integer>();
    private String activeTab;

    public Map<LabVessel, Integer> getVesselSampleSizeMap() {
        return vesselSampleSizeMap;
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public List<MercurySample> getFoundSamples() {
        return foundSamples;
    }

    public void setFoundSamples(List<MercurySample> foundSamples) {
        this.foundSamples = foundSamples;
    }

    public List<ProductOrder> getFoundPDOs() {
        return foundPDOs;
    }

    public void setFoundPDOs(List<ProductOrder> foundPDOs) {
        this.foundPDOs = foundPDOs;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public LabVessel getSelectedVessel() {
        return selectedVessel;
    }

    public void setSelectedVessel(LabVessel selectedVessel) {
        this.selectedVessel = selectedVessel;
    }

    public Boolean getShowPlateLayout() {
        return showPlateLayout;
    }

    public void setShowPlateLayout(Boolean showPlateLayout) {
        this.showPlateLayout = showPlateLayout;
    }

    public Boolean getShowSampleList() {
        return showSampleList;
    }

    public void setShowSampleList(Boolean showSampleList) {
        this.showSampleList = showSampleList;
    }

    public void listSearch(String barcode) {
        this.searchKey = barcode;
        listSearch();
    }

    public void listSearch() {
        List<String> searchList = cleanInputString();
        foundVessels = labVesselDao.findByListIdentifiers(searchList);
        foundSamples = mercurySampleDao.findBySampleKeys(searchList);
        foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
        for (LabVessel foundVessel : foundVessels) {
            vesselSampleSizeMap.put(foundVessel, foundVessel.getSampleInstances().size());
        }
    }

    private List<String> cleanInputString() {
        String[] keys = searchKey.split(",");
        int index = 0;
        for (String key : keys) {
            keys[index++] = key.trim();
        }
        return Arrays.asList(keys);
    }

    public void togglePlateLayout(LabVessel vessel) {
        selectedVessel = vessel;
        showPlateLayout = !showPlateLayout;
    }

    public void toggleSampleList(LabVessel vessel) {
        selectedVessel = vessel;
        showSampleList = !showSampleList;
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public String getOpenCloseValue(Boolean shown) {
        String value = "Open";
        if (shown) {
            value = "Close";
        }
        return value;
    }
}
