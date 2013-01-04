package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/search/plastic.action")
public class SearchActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/search/search.jsp";

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private LabBatchDAO labBatchDAO;

    @Validate(required = true, on = "search")
    private String searchKey;

    private List<LabVessel> foundVessels = null;
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private List<LabBatch> foundBatches;

    private boolean hasResults = false;
    private boolean onlyOneResult = false;

    @DefaultHandler
    @HandlesEvent("view")
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() throws Exception {
        List<String> searchList = cleanInputString(searchKey);

        foundVessels = labVesselDao.findByListIdentifiers(searchList);
        foundSamples = mercurySampleDao.findBySampleKeys(searchList);
        foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
        foundBatches = labBatchDAO.findByListIdentifier(searchList);

        long totalResults = foundVessels.size() + foundSamples.size() + foundPDOs.size() + foundBatches.size();

        // If there is only one result, jump to the item's page, if it has a view page
        if (totalResults == 1) {
            RedirectResolution resolution = getRedirectResolution();
            if (resolution != null) {
                return resolution;
            }
        }

        hasResults = totalResults > 0;
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    private RedirectResolution getRedirectResolution() {
        if (foundPDOs.size() > 0) {
            ProductOrder order = foundPDOs.get(0);
            return new RedirectResolution(ProductOrderActionBean.class, "view").addParameter("businessKey", order.getBusinessKey());
        }

        return null;
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

    public List<LabBatch> getFoundBatches() {
        return foundBatches;
    }

    public void setFoundBatches(List<LabBatch> foundBatches) {
        this.foundBatches = foundBatches;
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

    public boolean isHasResults() {
        return hasResults;
    }
}
