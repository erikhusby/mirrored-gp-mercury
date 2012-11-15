package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillingStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Named
@RequestScoped
public class ProductOrderBean {
    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private BSPUserList bspUserList;

    /** All product orders, fetched once and stored per-request (as a result of this bean being @RequestScoped). */
    private ProductOrderListModel allProductOrders;

    private ProductOrder[] selectedProductOrders;

    /**
     * Returns a list of all product orders. Only actually fetches the list from the database once per request
     * (as a result of this bean being @RequestScoped).
     *
     * @return list of all product orders
     */
    public ProductOrderListModel getAllProductOrders() {
        if (allProductOrders == null) {
            allProductOrders = new ProductOrderListModel(productOrderDao.findAll());
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
     * Returns a list of SelectItems for all product order sample statuses, including an "Any" selection.
     *
     * @return list of all research project statuses
     */
    public List<SelectItem> getAllSampleStatuses() {
        return BoundaryUtils.buildEnumFilterList(BillingStatus.values());
    }


    /**
     * Returns a list of SelectItems for all people who are owners of research projects.
     *
     * @return list of research project owners
     */
    public List<SelectItem> getAllProjectOwners() {
        Set<BspUser> owners = new HashSet<BspUser>();
        for (ProductOrder order : getAllProductOrders()) {
            Long createdBy = order.getCreatedBy();
            if (createdBy != null) {
                BspUser bspUser = bspUserList.getById(createdBy);
                if (bspUser != null) {
                    owners.add(bspUser);
                }
            }
        }

        return bspUserList.getSelectItems(owners);
    }

    /**
     * Simple finder to search for product order by key.  Allows Infrastructure level components the ability to use this
     * functionality
     * @param productOrderKey business key of the desired product order.
     * @return if found, and instance of the desired product order.  Otherwise null
     */
    public ProductOrder getProductOrderByKey( @Nonnull String productOrderKey) {
        return productOrderDao.findByBusinessKey(productOrderKey);
    }

    public ProductOrder[] getSelectedProductOrders() {
        return selectedProductOrders;
    }

    public void setSelectedProductOrders(ProductOrder[] selectedProductOrders) {
        this.selectedProductOrders = selectedProductOrders;
    }

    public String downloadLedgerUpdate() {
        return "got it";
    }
}
