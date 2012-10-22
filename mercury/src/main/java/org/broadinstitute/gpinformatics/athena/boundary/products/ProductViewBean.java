package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItemComparator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductComparator;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named("productView")
@RequestScoped
public class ProductViewBean {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    private Product product;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<Product> getAddOns() {
        if (product == null) {
            return new ArrayList<Product>();
        }

        ArrayList<Product> addOns = new ArrayList<Product>(product.getAddOns());
        Collections.sort(addOns, new ProductComparator());

        return addOns;
    }


    public List<PriceItem> getPriceItems() {
        if (product == null) {
            return new ArrayList<PriceItem>();
        }

        ArrayList<PriceItem> priceItems = new ArrayList<PriceItem>(product.getPriceItems());
        Collections.sort(priceItems, new PriceItemComparator());

        return priceItems;
    }


    public String getAvailabilityDate() {
        if (product == null || product.getAvailabilityDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(product.getAvailabilityDate());
    }


    public String getDiscontinuedDate() {
        if (product == null || product.getDiscontinuedDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(product.getDiscontinuedDate());
    }
}
