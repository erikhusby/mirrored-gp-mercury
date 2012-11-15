package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItemComparator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductComparator;
import org.broadinstitute.gpinformatics.athena.presentation.converter.ProductConverter;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductForm;
import org.broadinstitute.gpinformatics.infrastructure.jsf.TableData;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.New;
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

    @Inject
    private Conversation conversation;

    @Inject
    private TableData addOnData;

    public TableData getAddOnData() {
        return addOnData;
    }

    public void setAddOnData(TableData addOnData) {
        this.addOnData = addOnData;
    }

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

    private UIInput viewParam;

    public UIInput getViewParam() {
        return viewParam;
    }

    public void setViewParam(UIInput viewParam) {
        this.viewParam = viewParam;
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
    private ProductConverter productConverter;

    public void initView() {
        if (!facesContext.isPostback()) {
            addOnData.setValues(new ArrayList<Product>(product.getAddOns()));
            if (conversation.isTransient()) {
                conversation.begin();
            }
        }
    }

    public Product getProduct() {
//        loadProduct();
        return product;
    }

    public void loadProduct() {
        // if the model is not set but we have the business key in our cache binding, run the converter
        // to generate the model.  we expect to be in this case for every ajax request on this page.
        if (product == null && vsBusinessKeyCache != null && vsBusinessKeyCache.getValue() != null) {
            product = productConverter.getAsObject((String) vsBusinessKeyCache.getValue());
        }
        // if we have the model but have not yet set the business key into our cache binding, set the key
        // into the cache binding.  we expect to be in this case on the initial page render when the f:param
        // has run the productConverter to set the model into this backing bean
        else if (vsBusinessKeyCache != null && vsBusinessKeyCache.getValue() == null && product != null) {
            vsBusinessKeyCache.setValue(product.getBusinessKey());
        }
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public List<Product> getAddOns() {
//        loadProduct();

        if (product != null && addOns == null) {
            addOns = new ArrayList<Product>();
            addOns.addAll(product.getAddOns());
            // TODO make ProductComparator a static field on Product
            Collections.sort(addOns, new ProductComparator());
        }

        return addOns;
    }

    public List<PriceItem> getPriceItems() {
//        loadProduct();

        if (product != null && priceItems == null) {
            priceItems = new ArrayList<PriceItem>();
            priceItems.addAll(product.getPriceItems());
            // TODO make PriceItemComparator a static field on PriceItem
            Collections.sort(priceItems, new PriceItemComparator());
        }

        return priceItems;
    }


    public void onPreRenderView() throws IOException {
//        loadProduct();

        if ( product == null ) {
            addErrorMessage("No product with this part number exists.");
            facesContext.renderResponse();
        }
    }

    public String getAvailabilityDate() {
        loadProduct();

        if (product == null || product.getAvailabilityDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(product.getAvailabilityDate());
    }


    public String getDiscontinuedDate() {
        loadProduct();

        if (product == null || product.getDiscontinuedDate() == null) {
            return "";
        }

        return DATE_FORMAT.format(product.getDiscontinuedDate());
    }


    public Integer getExpectedCycleTimeDays() {
        loadProduct();
        return ProductForm.convertCycleTimeSecondsToDays(product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        loadProduct();
        product.setExpectedCycleTimeSeconds(ProductForm.convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        loadProduct();
        return ProductForm.convertCycleTimeSecondsToDays(product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        loadProduct();
        product.setGuaranteedCycleTimeSeconds(ProductForm.convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
    }

}
