package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductForm;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIInput;
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

    /**
     * Model object
     */
    private Product product;

    /**
     * Cache used to store the business key in view state so it will survive across AJAX sort requests
     */
    private UIInput vsBusinessKeyCache;

    public UIInput getVsBusinessKeyCache() {
        return vsBusinessKeyCache;
    }

    public void setVsBusinessKeyCache(UIInput vsBusinessKeyCache) {
        this.vsBusinessKeyCache = vsBusinessKeyCache;
    }

    /**
     * Breaking out add-ons to its own field both because it needs to be a List to be fed to DataTable, plus
     * we need to give DataTable the same instance each time during the lifecycle so it can sort it
     */
    private List<Product> addOns;

    /**
     * Breaking out priceItems to its own field both because it needs to be a List to be fed to DataTable, plus
     * we need to give DataTable the same instance each time during the lifecycle so it can sort it
     */
    private List<PriceItem> priceItems;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ProductDao productDao;


    private String getCachedBusinessKey() {
        if (vsBusinessKeyCache != null) {
            return (String) vsBusinessKeyCache.getValue();
        }
        return null;
    }

    public void setCachedBusinessKey(String key) {
        vsBusinessKeyCache.setValue(key);
    }


    public Product getProduct() {

        // if the model is not set but we have the business key in our cache, use the dao to get the Product
        // for this business key.  we expect to be in this case for every ajax request on this page.
        //
        // else if we have the model but have not yet set the business key into our cache, set the key
        // into the cache.  we expect to be in this case on the initial page render when the f:viewParam
        // has run the productConverter to set the model into this backing bean
        if (product == null && getCachedBusinessKey() != null) {
            product = productDao.findByPartNumber(getCachedBusinessKey());
        }
        else if (product != null && getCachedBusinessKey() == null) {
            setCachedBusinessKey(product.getBusinessKey());
        }

        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<Product> getAddOns() {
        if (addOns == null) {
            addOns = new ArrayList<Product>(getProduct().getAddOns());
            Collections.sort(addOns);
        }
        return addOns;
    }

    public List<PriceItem> getPriceItems() {
        if (priceItems == null) {
            priceItems = new ArrayList<PriceItem>(getProduct().getPriceItems());
            Collections.sort(priceItems);
        }
        return priceItems;
    }


    public void onPreRenderView() throws IOException {
        if (getProduct() == null) {
            addErrorMessage("No product with this part number exists.");
            facesContext.renderResponse();
        }
    }

    public String getAvailabilityDate() {
        if (getProduct().getAvailabilityDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(getProduct().getAvailabilityDate());
    }


    public String getDiscontinuedDate() {
        if (getProduct().getDiscontinuedDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(getProduct().getDiscontinuedDate());
    }


    public Integer getExpectedCycleTimeDays() {
        return ProductForm.convertCycleTimeSecondsToDays(getProduct().getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        getProduct().setExpectedCycleTimeSeconds(ProductForm.convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        return ProductForm.convertCycleTimeSecondsToDays(getProduct().getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        getProduct().setGuaranteedCycleTimeSeconds(ProductForm.convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
    }

}
