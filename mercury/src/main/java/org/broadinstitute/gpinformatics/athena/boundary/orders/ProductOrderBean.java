package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.boundary.BoundaryUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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


    public List<SelectItem> getAllProductNames() {

        // only show products that are actually referenced in orders -- is that reasonable or do we really want all products?
        List<ProductOrder> productOrders = productOrderDao.findAll(ProductOrderDao.FetchSpec.Product);

        SortedSet<String> productNames = new TreeSet<String>();
        for (ProductOrder productOrder : productOrders) {
            productNames.add(productOrder.getProduct().getProductName());
        }

        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (String productName : productNames) {
            items.add(new SelectItem(productName));
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
