package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillingStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Named
@RequestScoped
public class ProductOrderBean {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

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

    public List<SelectItem> getAllProductNames() {
        Set<String> names = productDao.getProductNames();

        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (String name : names) {
            items.add(new SelectItem(name));
        }

        return items;
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
}
