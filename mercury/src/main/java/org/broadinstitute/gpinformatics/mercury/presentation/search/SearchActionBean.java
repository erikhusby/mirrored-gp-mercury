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
import java.util.Map;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/search/all.action")
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
    private boolean multipleResultTypes = false;
    private Map<String, String> getPDOKeyMap = null;
    private Map<String, String> getIndexesMap = null;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent("search")
    public Resolution search() throws Exception {
        List<String> searchList = cleanInputString(searchKey);

        int count = 0;
        long totalResults = 0l;

        foundVessels = labVesselDao.findByListIdentifiers(searchList);
        if (foundVessels.size() > 0) {
            count++;
            totalResults += foundVessels.size();
        }

        foundSamples = mercurySampleDao.findBySampleKeys(searchList);
        if (foundSamples.size() > 0) {
            count++;
            totalResults += foundSamples.size();
        }

        foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
        if (foundPDOs.size() > 0) {
            count++;
            totalResults += foundPDOs.size();
        }

        foundBatches = labBatchDAO.findByListIdentifier(searchList);
        if (foundBatches.size() > 0) {
            count++;
            totalResults += foundBatches.size();
        }

        // If there is only one result, jump to the item's page, if it has a view page
        if (totalResults == 1) {
            RedirectResolution resolution = getRedirectResolution();
            if (resolution != null) {
                return resolution;
            }
        }

        multipleResultTypes = count > 1;
        hasResults = totalResults > 0;

        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    private RedirectResolution getRedirectResolution() {
        if (foundPDOs.size() > 0) {
            ProductOrder order = foundPDOs.get(0);
            return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter("productOrder", order.getBusinessKey());
        }

        return null;
    }

    public boolean isMultipleResultTypes() {
        return multipleResultTypes;
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

    public String getResultTypeStyle() {
        if (multipleResultTypes) {
            return "display:none";
        }

        return "display:block";
    }

    public Map<String, String> getGetPDOKeyMap() {
        if (getPDOKeyMap == null) {

        }

        return getPDOKeyMap;
    }

    public Map<String, String> getGetIndexesMap() {
        if (getIndexesMap == null) {

        }

        return getIndexesMap;
    }
}
