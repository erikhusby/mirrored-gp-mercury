package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * This is the bean class for the generic search page.
 */
@ManagedBean
@ViewScoped
public class SearchBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private LabBatchDAO labBatchDAO;

    private String searchKey;
    private List<LabVessel> foundVessels;
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private List<LabBatch> foundBatches;


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

    public List<LabBatch> getFoundBatches() {
        return foundBatches;
    }

    public void setFoundBatches(List<LabBatch> foundBatches) {
        this.foundBatches = foundBatches;
    }

    public void listSearch(String barcode) {
        this.searchKey = barcode;
        listSearch();
    }

    /**
     * This method searches all of the various DAOs to get their respective entities based on the search keys.
     */
    public void listSearch() {
        List<String> searchList = cleanInputString(searchKey);
        foundVessels = labVesselDao.findByListIdentifiers(searchList);
        foundSamples = mercurySampleDao.findBySampleKeys(searchList);
        foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
        foundBatches = labBatchDAO.findByListIdentifier(searchList);
    }

    /**
     * This method takes a list of search keys turns newlines into commas and splits the individual search keys into
     * a list.
     *
     * @return A list of all the keys from the searchKey string.
     */
    private static List<String> cleanInputString(String searchKey) {
        searchKey = searchKey.replaceAll("\\n", ",");
        String[] keys = searchKey.split(",");
        int index = 0;
        for (String key : keys) {
            keys[index++] = key.trim();
        }
        return Arrays.asList(keys);
    }
}
