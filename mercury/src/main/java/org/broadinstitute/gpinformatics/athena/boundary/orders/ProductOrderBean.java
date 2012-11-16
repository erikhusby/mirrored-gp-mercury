package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillingStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.annotation.Nonnull;
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

    private ProductOrder[] selectedProductOrders;

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

}
