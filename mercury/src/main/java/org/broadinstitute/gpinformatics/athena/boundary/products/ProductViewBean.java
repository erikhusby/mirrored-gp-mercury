package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItemComparator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductComparator;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductForm;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named("productView")
@RequestScoped
public class ProductViewBean extends AbstractJsfBean {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");

    private Product product;

    @Inject
    private FacesContext facesContext;

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

    public void onPreRenderView() throws IOException {
        if ( product == null ) {
            addErrorMessage("No product with this part number exists.", "The product part number does not exist.");
            facesContext.renderResponse();
        }
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


    public Integer getExpectedCycleTimeDays() {
        return ProductForm.convertCycleTimeSecondsToDays(product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        product.setExpectedCycleTimeSeconds( ProductForm.convertCycleTimeDaysToSeconds(expectedCycleTimeDays) );
    }

    public Integer getGuaranteedCycleTimeDays() {
        return ProductForm.convertCycleTimeSecondsToDays(product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        product.setGuaranteedCycleTimeSeconds( ProductForm.convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays) );
    }

}
