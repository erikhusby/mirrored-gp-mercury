package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillingStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@Named
@RequestScoped
public class ProductOrderBean {
    @Inject
    private ProductOrderDao productOrderDao;

    /** All product orders, fetched once and stored per-request (as a result of this bean being @RequestScoped). */
    private List<ProductOrder> allProductOrders;

    private List<ProductOrder> filteredProductOrders;

    /**
     * Returns a list of all product orders. Only actually fetches the list from the database once per request
     * (as a result of this bean being @RequestScoped).
     *
     * @return list of all product orders
     */
    public List<ProductOrder> getAllProductOrders() {
        if (allProductOrders == null) {
            allProductOrders = productOrderDao.findAll();
        }
        return allProductOrders;
    }

    /**
     * Returns a list of SelectItems for all product order statuses, including an "Any" selection.
     *
     * @return list of all research project statuses
     */
    public List<SelectItem> getAllOrderStatuses() {
        return BoundaryUtils.buildEnumFilterList(ProductOrder.OrderStatus.values());
    }


    /**
     * Returns a list of SelectItems for all project statuses, including an "Any" selection.
     *
     * @return list of all research project statuses
     */
    public List<SelectItem> getAllProjectStatuses() {
        return BoundaryUtils.buildEnumFilterList(ResearchProject.Status.values());
    }

    /**
     * Returns a list of SelectItems for all product order sample statuses, including an "Any" selection.
     *
     * @return list of all research project statuses
     */
    public List<SelectItem> getAllSampleStatuses() {
        return BoundaryUtils.buildEnumFilterList(BillingStatus.values());
    }

    public List<ProductOrder> getFilteredProductOrders() {
        return filteredProductOrders;
    }

    public void setFilteredProductOrders(List<ProductOrder> filteredProductOrders) {
        this.filteredProductOrders = filteredProductOrders;
    }
}
