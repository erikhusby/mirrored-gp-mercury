package org.broadinstitute.gpinformatics.athena.presentation.search;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean;

import javax.inject.Inject;

/**
 * This handles all the needed interface processing elements.
 */
@UrlBinding(SearchActionBean.ACTIONBEAN_URL_BINDING)
public final class SearchActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/search.action";

    protected enum SearchType {
        PDO_BY_KEY, RP_BY_KEY, P_BY_KEY,
    }

    /**
     * Action for handling when user enters search text in navigation form search textfield.
     */
    public static final String QUICK_SEARCH_ACTION = "quickSearch";

    @Inject
    private UserBean userBean;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    @Validate(required = true, on = {QUICK_SEARCH_ACTION})
    private String searchKey;

    private class SearchResult<T> {
        SearchType searchType;
        BusinessObject<T> businessObject;
        Class<? extends CoreActionBean> actionBeanClass;
        String parameter;

        public SearchResult(SearchType searchType, BusinessObject<T> businessObject,
                            Class<? extends CoreActionBean> actionBeanClass,
                            String parameter) {
            this.searchType = searchType;
            this.businessObject = businessObject;
            this.actionBeanClass = actionBeanClass;
            this.parameter = parameter;
        }

    }

    @DefaultHandler
    @HandlesEvent(QUICK_SEARCH_ACTION)
    public Resolution quickSearch() throws Exception {
        // All business key prefixes use uppercase
        searchKey = searchKey.toUpperCase();

        SearchResult searchResult = doSearch();

        if (searchResult != null) {
            return new RedirectResolution(searchResult.actionBeanClass, "view")
                    .addParameter(searchResult.parameter, searchResult.businessObject.getBusinessKey());
        }

        // Did not find anything matching the search string so it could have been a bad prefix, bad business key or
        // whatever.  This happens when we've looked through all search types and nothing has been found.  Allowing
        // this to be null ensures we don't have to encode any prefix string in order determine what kind of search
        // it is, but keeps things generic.
        addGlobalValidationError("There were no matching items for your quick search '" + searchKey + "'.");

        // Can't just stay where you are and report back the error because page could have been submitting data and
        // reloading it can cause side effects (or not, it is unknown).  Add any known parameters back on.
        return new RedirectResolution(SecurityActionBean.HOME_PAGE)
                .addParameters(getContext().getRequest().getParameterMap()).flash(this);
    }


    /**
     * Perform a search to identify a singular result for a quick search to lookup a particular item.  This is not
     * designed to handle returning multiple things of various types but will keep looking though different types
     * of objects until it finds something.
     */
    private SearchResult doSearch() {
        SearchResult searchResult = null;
        searchKey = searchKey.trim();

        for (SearchType searchForItem : SearchType.values()) {
            switch (searchForItem) {
            case PDO_BY_KEY:
                ProductOrder productOrder = productOrderDao.findByBusinessKey(searchKey);
                if (productOrder != null) {
                    searchResult = new SearchResult(SearchType.PDO_BY_KEY, productOrder, ProductOrderActionBean.class,
                            ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
                }

                break;
            case P_BY_KEY:
                Product product = productDao.findByBusinessKey(searchKey);
                if (product != null) {
                    searchResult = new SearchResult(SearchType.P_BY_KEY, product, ProductActionBean.class,
                            ProductActionBean.PRODUCT_PARAMETER);
                }

                break;
            case RP_BY_KEY:
                ResearchProject project = researchProjectDao.findByBusinessKey(searchKey);
                if (project != null) {
                    searchResult = new SearchResult(SearchType.RP_BY_KEY, project, ResearchProjectActionBean.class,
                            ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER);
                }

                break;
            }

            if (searchResult != null) {
                // Something was found so break out ASAP to get the results back to the user.
                break;
            }
        }

        return searchResult;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }
}
