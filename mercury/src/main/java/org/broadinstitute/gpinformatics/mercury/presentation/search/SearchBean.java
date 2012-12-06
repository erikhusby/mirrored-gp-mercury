package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@ManagedBean
@ViewScoped
public class SearchBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private ProductOrderDao productOrderDao;

    private String searchKey;
    private MercurySample selectedSample;
    private List<LabVessel> foundVessels;
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private Boolean showPlasticView = false;


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

    public MercurySample getSelectedSample() {
        return selectedSample;
    }

    public void setSelectedSample(MercurySample selectedSample) {
        this.selectedSample = selectedSample;
    }

    public Boolean getShowPlasticView() {
        return showPlasticView;
    }

    public void setShowPlasticView(Boolean showPlasticView) {
        this.showPlasticView = showPlasticView;
    }

    public void listSearch(String barcode) {
        this.searchKey = barcode;
        listSearch();
    }

    public void listSearch() {
        List<String> searchList = cleanInputString();
        foundVessels = labVesselDao.findByListIdentifiers(searchList);
        foundSamples = mercurySampleDao.findBySampleKeys(searchList);
        //   foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
    }

    private List<String> cleanInputString() {
        String[] keys = searchKey.split(",");
        int index = 0;
        for (String key : keys) {
            keys[index++] = key.trim();
        }
        return Arrays.asList(keys);
    }
}
