package org.broadinstitute.gpinformatics.athena.boundary.search;


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

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * This class is for handling searches for various items like products, orders, research projects.
 */
@Stateful
@RequestScoped
public class SearchEjb {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductDao productDao;

    protected enum SearchType {
        ProductOrderByKey, ResearchProjectByKey, ProductByKey,
    }

    /**
     * Perform a search to identify a singular result for a quick search to lookup a particular item.  This is not
     * designed to handle returning multiple things of various types but will keep looking through different types
     * of objects until it finds something and returns the first thing it finds.
     *
     * @param searchKey the search string
     *
     * @return the {@link SearchResult} match from the search or null if nothing found
     */
    public SearchResult search(String searchKey) {
        searchKey = searchKey.trim();

        for (SearchType searchForItem : SearchType.values()) {
            switch (searchForItem) {
            case ProductOrderByKey:
                ProductOrder productOrder = productOrderDao.findByBusinessKey(searchKey);
                if (productOrder != null) {
                    return new SearchResult(SearchType.ProductOrderByKey, productOrder, ProductOrderActionBean.class,
                            ProductOrderActionBean.PRODUCT_ORDER_PARAMETER);
                }

                break;
            case ProductByKey:
                Product product = productDao.findByBusinessKey(searchKey);
                if (product != null) {
                    return new SearchResult(SearchType.ProductByKey, product, ProductActionBean.class,
                            ProductActionBean.PRODUCT_PARAMETER);
                }

                break;
            case ResearchProjectByKey:
                ResearchProject project = researchProjectDao.findByBusinessKey(searchKey);
                if (project != null) {
                    return new SearchResult(SearchType.ResearchProjectByKey, project, ResearchProjectActionBean.class,
                            ResearchProjectActionBean.RESEARCH_PROJECT_PARAMETER);
                }

                break;
            }
        }

        // Nothing was found!
        return null;
    }

    /**
     * The resulting object from performing a search.
     */
    public class SearchResult<T> {
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

        public SearchType getSearchType() {
            return searchType;
        }

        public BusinessObject<T> getBusinessObject() {
            return businessObject;
        }

        public Class<? extends CoreActionBean> getActionBeanClass() {
            return actionBeanClass;
        }

        public String getParameter() {
            return parameter;
        }
    }
}
