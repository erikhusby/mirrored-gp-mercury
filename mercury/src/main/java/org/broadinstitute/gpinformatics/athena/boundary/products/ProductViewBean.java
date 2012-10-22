package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        Collections.sort(addOns, new Comparator<Product>() {
            @Override
            public int compare(Product priceItem, Product priceItem1) {
                CompareToBuilder builder = new CompareToBuilder();
                return builder.build();
            }
        });

        return addOns;
    }


    public List<PriceItem> getPriceItems() {
        if (product == null) {
            return new ArrayList<PriceItem>();
        }

        ArrayList<PriceItem> priceItems = new ArrayList<PriceItem>(product.getPriceItems());
        Collections.sort(priceItems, new Comparator<PriceItem>() {
            @Override
            public int compare(PriceItem priceItem, PriceItem priceItem1) {
                CompareToBuilder builder = new CompareToBuilder();
                builder.append(priceItem.getPlatform(), priceItem1.getPlatform());
                builder.append(priceItem.getCategory(), priceItem1.getCategory());
                builder.append(priceItem.getName(), priceItem1.getName());

                return builder.build();
            }
        });

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
