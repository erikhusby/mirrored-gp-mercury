package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This handles all the needed interface processing elements.
 */
@UrlBinding(SearchActionBean.ACTIONBEAN_URL_BINDING)
public class SearchActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/all.action";

    protected enum SearchType {
        PDO_BY_KEY, RP_BY_KEY, P_BY_KEY, BATCH_BY_KEY, VESSELS_BY_BARCODE
    }

    private static final String SEPARATOR = ",";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[" + SEPARATOR + "\\s]+");

    /**
     * Automatically convert known BSP IDs (SM-, SP-) to uppercase.
     */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[sS][mMpP]-.*");

    private static final String SEARCH_LIST_PAGE = "/search/search.jsp";
    public static final String SEARCH_ACTION = "search";

    /**
     * Action for handling when user enters search text in navigation form search textfield.
     */
    public static final String QUICK_SEARCH_ACTION = "quickSearch";

    @Inject
    private UserBean userBean;

    @Inject
    private LabBatchDao labBatchDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Validate(required = true,
            on = {SEARCH_ACTION, QUICK_SEARCH_ACTION})
    private String searchKey;

    private Set<LabVessel> foundVessels = new HashSet<>();
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private List<LabBatch> foundBatches;

    private boolean resultsAvailable = false;
    private boolean multipleResultTypes = false;

    private boolean isSearchDone = false;
    private int numSearchTerms;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SEARCH_LIST_PAGE);
    }

    protected void doSearch(SearchType... searchForItems) {
        if (searchForItems.length == 0) {
            searchForItems = SearchType.values();
        }

        List<String> searchList = cleanInputString(searchKey);
        numSearchTerms = searchList.size();
        int count = 0;
        long totalResults = 0l;

        for (SearchType searchForItem : searchForItems) {
            switch (searchForItem) {
            case VESSELS_BY_BARCODE:
                foundVessels.addAll(labVesselDao.findByListIdentifiers(searchList));
                if (!foundVessels.isEmpty()) {
                    count++;
                    totalResults += foundVessels.size();
                }
                break;
            case BATCH_BY_KEY:
                foundBatches = labBatchDao.findByListIdentifier(searchList);
                if (!foundBatches.isEmpty()) {
                    count++;
                    totalResults += foundBatches.size();
                }
                break;
            }
        }

        multipleResultTypes = count > 1;
        resultsAvailable = totalResults > 0;
        isSearchDone = true;
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() throws Exception {
        doSearch();
        return new ForwardResolution(SEARCH_LIST_PAGE);
    }

    private RedirectResolution getRedirectResolution() {
        if (foundPDOs.size() > 0) {
            ProductOrder order = foundPDOs.get(0);
            return new RedirectResolution(ProductOrderActionBean.class,
                    ProductOrderActionBean.VIEW_ACTION).addParameter("productOrder", order.getBusinessKey());
        }

        return null;
    }

    public boolean isMultipleResultTypes() {
        return multipleResultTypes;
    }

    public Set<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(Set<LabVessel> foundVessels) {
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

    public static List<String> cleanInputString(String searchKey) {
        return cleanInputString(searchKey, false);
    }

    public static List<String> cleanInputStringForSamples(String searchKey) {
        return cleanInputString(searchKey, true);
    }

    /**
     * This method takes a list of search keys turns newlines into commas and splits the individual search keys into
     * a list.
     *
     * @return {@link List<String>} of all the keys from the searchKey string
     */
    private static List<String> cleanInputString(String searchKey, boolean includeSampleFixup) {
        if (searchKey == null) {
            return Collections.emptyList();
        }

        String[] valueArray = SPLIT_PATTERN.split(searchKey, 0);
        if (valueArray.length == 1 && valueArray[0].isEmpty()) {
            // Handle empty string case.
            valueArray = new String[0];
        }

        List<String> sampleIds = new ArrayList<>(valueArray.length);
        for (String value : valueArray) {
            if (!StringUtils.isBlank(value)) {
                value = value.trim();
                if (includeSampleFixup && UPPERCASE_PATTERN.matcher(value).matches()) {
                    value = value.toUpperCase();
                }
                sampleIds.add(value);
            }
        }

        return sampleIds;
    }

    public boolean isResultsAvailable() {
        return resultsAvailable;
    }

    public String getResultTypeStyle() {
        if (multipleResultTypes) {
            return "display:none";
        }

        return "display:block";
    }

    protected LabVesselDao getLabVesselDao() {
        return labVesselDao;
    }

    public boolean isSearchDone() {
        return isSearchDone;
    }

    public void setSearchDone(boolean searchDone) {
        isSearchDone = searchDone;
    }

    public int getNumSearchTerms() {
        return numSearchTerms;
    }

    public void setNumSearchTerms(int numSearchTerms) {
        this.numSearchTerms = numSearchTerms;
    }
}
