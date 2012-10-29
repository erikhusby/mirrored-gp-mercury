package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductComparator;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.util.*;

@Named
@RequestScoped
public class ProductForm extends AbstractJsfBean {

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private ProductBoundary productBoundary;

    /**
     * Source of quote server sourced price data
     */
    @Inject
    private PriceListCache priceListCache;

    @Inject
    private Log logger;

    /**
     * Holder of long-running conversation state needed for price items
     */
    @Inject
    private ProductFormConversationData conversationData;

    @Inject
    private transient FacesContext facesContext;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    public static final int ONE_HOUR_IN_SECONDS = 3600;
    private Product product;

    /**
     * JAXB {@link PriceItem} DTOs
     */
    private List<PriceItem> priceItems;

    /**
     * JAXB {@link PriceItem} DTOs, there can be only one default price item but this is a list so the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete} can be styled consistently with all the other
     * multi-selecting {@link org.primefaces.component.autocomplete.AutoComplete}s in Mercury
     */
    private List<PriceItem> defaultPriceItems = new ArrayList<PriceItem>();

    private PriceItem defaultPriceItem;

    private List<Product> addOns;

    /**
     * Hook for the preRenderView event that initiates the long running conversation and sets up conversation scoped
     * data from the product, also initializes the form as appropriate
     */
    public void onPreRenderView() {
        conversationData.beginConversation(product);
        initForm();
    }


    /**
     * Utility method to map JAXB DTOs to entities for price items
     * @param priceItem
     * @return
     */
    private org.broadinstitute.gpinformatics.athena.entity.products.PriceItem dtoToEntity(PriceItem priceItem) {
        return new org.broadinstitute.gpinformatics.athena.entity.products.PriceItem(
                priceItem.getId(),
                priceItem.getPlatformName(),
                priceItem.getCategoryName(),
                priceItem.getName());
    }

    /**
     * Convenience method to differentiate between create and edit use cases
     * @return
     */
    public boolean isCreating() {
        return product.getPartNumber() == null;
    }

    /**
     * Initialize the form if this is not a postback
     */
    private void initForm() {
        if (!facesContext.isPostback()) {
            // Use ID instead of partNumber, which can be non-empty if the user enters one on create!
//            if ((product.getPartNumber() != null) && !StringUtils.isBlank(product.getPartNumber())) {
            if (product.getProductId() == null) {
//                priceItems = new ArrayList<PriceItem>();
//                addOns = new ArrayList<Product>();
            } else {
                // Don't load product here... let the converter handle that
//                product = productDao.findByBusinessKey(product.getPartNumber());
                if (product.getPriceItems() != null) {
                    priceItems = new ArrayList<PriceItem>();
                    for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : product.getPriceItems()) {
                        priceItems.add(entityToDto(priceItem));
                    }
                }
                if (product.getDefaultPriceItem() != null) {
                    defaultPriceItem = entityToDto(product.getDefaultPriceItem());
                }
                // TODO: is this needed? or does the actual backing model work for p:autoComplete?
                addOns.addAll(product.getAddOns());
            }
        }
    }

    /**
     * maps between entity and JAXB DTOs for price items
     * @param entity
     * @return
     */
    private PriceItem entityToDto(org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity) {
        return new PriceItem(entity.getQuoteServerId(), entity.getPlatform(), entity.getCategory(), entity.getName());
    }

    /**
     * Initialze an empty {@link Product} so fields can drill into the backing product without NPEs
     */
    public void initEmptyProduct() {
        product = new Product(null, null, null, null, null, null, null,
                null, null, null, null, null, DEFAULT_TOP_LEVEL, DEFAULT_WORKFLOW_NAME);
    }

    /**
     * Enumerate the product families
     * @return
     */
    public List<ProductFamily> getProductFamilies() {
        return  productFamilyDao.findAll();
    }


//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String save() {
        boolean validationPassed = true;

        if (!isPartNumberUnique()) {
            String errorMessage = MessageFormat.format("The Product Part-Number ''{0}'' is not unique.", product.getPartNumber());
            addErrorMessage("partNumber", errorMessage, errorMessage);
        }

        if (! dateRangeOkay()) {
            String errorMessage = "Availability date must precede discontinued date.";
            addErrorMessage("Date range invalid", errorMessage, errorMessage);
            validationPassed = false;
        }
        if (priceItems.isEmpty()) {
            addErrorMessage("priceItem", "Price Items is required.", "Price Items is required.");
            validationPassed = false;
        }
        if (getDefaultPriceItem() == null) {
            addErrorMessage("defaultPriceItem", "Default Price Item is required.", "Default Price Item is required.");
            validationPassed = false;
        }

        if (!validationPassed) {
            return null;
        }

//        ProductForm thisEjb = sessionContext.getBusinessObject(ProductForm.class);
        if (product.getProductId() == null ) {
//            return create();
            return productBoundary.create();
//            return thisEjb.create();
        } else {
//            return edit();
            return productBoundary.edit();
//            return thisEjb.edit();
        }
    }

    private boolean isPartNumberUnique() {
        Product existingProduct = productDao.findByPartNumber(product.getPartNumber());
        if (existingProduct != null && existingProduct.getProductId() != product.getProductId()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Sanity check dates
     * @return
     */
    private boolean dateRangeOkay() {
        if ((product.getAvailabilityDate() != null ) &&
                (product.getDiscontinuedDate() != null ) &&
                (product.getAvailabilityDate().after(product.getDiscontinuedDate()))) {
            return false;
        }
        return true;
    }

//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String create() {
        try {
            addAllAddOnsToProduct();
            addAllPriceItemsToProduct();

            productDao.persist(product);
            addInfoMessage("Product created.", "Product " + product.getPartNumber() + " has been created.");
        } catch (Exception e ) {
            logger.error("Exception while persisting Product: " + e);
            String errorMessage = "Exception occurred - " + e.getMessage();
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The Product Part-Number ''{0}'' is not unique.", product.getPartNumber());
            }
            addErrorMessage("Product not Created.", errorMessage, errorMessage + ": " + e);
            return "create";
        }
        return redirect("list");
    }

//    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String edit() {
        try {
            addAllAddOnsToProduct();
            addAllPriceItemsToProduct();

            productDao.getEntityManager().merge(getProduct());
            addInfoMessage("Product detail updated.", "Product " + getProduct().getPartNumber() + " has been updated.");
        } catch (Exception e ) {
            String errorMessage = "Exception occurred - " + e.getMessage();
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The Product Part-Number ''{0}'' is not unique.", product.getPartNumber());
            }
            addErrorMessage("Product not updated.", errorMessage, errorMessage + ": " + e);
            return "create";
        }
        return redirect("list");
    }

    /**
     * Entify all the addons from our JAXB DTOs and add them to the {@link Product} before persisting
     */
    private void addAllAddOnsToProduct() {
        Date now = Calendar.getInstance().getTime();
        if ( addOns != null) {
            for ( Product aProductAddOn : addOns ) {
                if ( aProductAddOn != null ) {
                    if ( aProductAddOn.isAvailable() || aProductAddOn.getAvailabilityDate().after( now ) ) {
                        product.addAddOn( aProductAddOn );
                    } else {
                        throw new RuntimeException("Product AddOn " + aProductAddOn.getPartNumber() + " is no longer available. Please remove it from the list.");
                    }
                }
            }
        }
    }


    /**
     * Utility method to grab a persistent/detached JPA entity corresponding to this JAXB DTO if one exists,
     * otherwise return just a transient JPA entity
     *
     * @param priceItem
     * @return
     */
    private org.broadinstitute.gpinformatics.athena.entity.products.PriceItem findEntity(PriceItem priceItem) {
        // quite sure this is not the right way to do this, restructure as necessary
        org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity =
                priceItemDao.find(priceItem.getPlatformName(), priceItem.getCategoryName(), priceItem.getName());

        if (entity == null) {
            entity = dtoToEntity(priceItem);
        }

        return entity;
    }


    /**
     * Entify all the price items from our JAXB DTOs and add them to the {@link Product} before persisting
     */
    private void addAllPriceItemsToProduct() {
        product.getPriceItems().clear();

        for (PriceItem priceItem : priceItems) {

            org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity = findEntity(priceItem);
            product.addPriceItem(entity);

            if (defaultPriceItem.equals(priceItem)) {
                product.setDefaultPriceItem(entity);
            }
        }
    }

    public Product getProduct() {
        return product;
    }
    public void setProduct(final Product product) {
        this.product = product;
    }

    public Integer getExpectedCycleTimeHours() {
        return convertCycleTimeSecondsToHours (product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeHours(final Integer expectedCycleTimeHours) {
        product.setExpectedCycleTimeSeconds(convertCycleTimeHoursToSeconds(expectedCycleTimeHours));
    }

    public Integer getGuaranteedCycleTimeHours() {
        return convertCycleTimeSecondsToHours (product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeHours(final Integer guaranteedCycleTimeHours) {
        product.setGuaranteedCycleTimeSeconds(convertCycleTimeHoursToSeconds(guaranteedCycleTimeHours));
    }


    public List<Product> getAddOns() {
        if (product == null) {
            return new ArrayList<Product>();
        }
        ArrayList<Product> addOns = new ArrayList<Product>(product.getAddOns());
        Collections.sort(addOns, new ProductComparator());
        return addOns;
    }


    public void setAddOns(final List<Product> addOns) {
        this.addOns = addOns;
    }


    /**
     * Pull {@link PriceItem} data from conversation scoped {@link ProductFormConversationData}
     *
     * @return
     */
    public List<PriceItem> getPriceItems() {
        return priceItems;
    }

    /**
     * NOOP, this is required for the PrimeFaces {@link org.primefaces.component.autocomplete.AutoComplete}, but the
     * actual setting of {@link PriceItem}s is handled in the ajax event listener only
     * @param priceItems
     */
    public void setPriceItems(List<PriceItem> priceItems) {
        this.priceItems = priceItems;
    }


    /**
     * Converts cycle times from hours to seconds.
     * @param cycleTimeHours
     * @return the number of seconds.
     */
    public static Integer convertCycleTimeHoursToSeconds(Integer cycleTimeHours) {
        Integer cycleTimeSeconds = null;
        if ( cycleTimeHours != null ) {
            cycleTimeSeconds = ( cycleTimeHours == null ? 0 : cycleTimeHours.intValue() * ONE_HOUR_IN_SECONDS);
        }
        return cycleTimeSeconds;
    }

    /**
     * Converts cycle times from seconds to hours.
     * This method rounds down to the nearest hour
     * @param cycleTimeSeconds
     * @return the number of hours.
     */
    public static Integer convertCycleTimeSecondsToHours(Integer cycleTimeSeconds) {
        Integer cycleTimeHours = null;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_HOUR_IN_SECONDS ) {
            cycleTimeHours =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_HOUR_IN_SECONDS)) / ONE_HOUR_IN_SECONDS;
        }
        return cycleTimeHours;
    }


    /**
     * AJAX unselection (removal) handler for {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param unselectEvent
     */
    public void onPriceItemUnselect(UnselectEvent unselectEvent) {
        PriceItem priceItem = (PriceItem) unselectEvent.getObject();
//        priceItems.remove(priceItem);
        conversationData.getDefaultPriceItems().remove(priceItem);

        // nuke out the default price item if it was the same price item we just removed
        if (defaultPriceItem != null && defaultPriceItem.equals(priceItem)) {
            defaultPriceItem = null;
        }
    }

    /**
     * AJAX selection (addition) handler for default {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param selectEvent
     */

    public void onPriceItemSelect(SelectEvent selectEvent) {
//        priceItems.add((PriceItem) selectEvent.getObject());
        getDefaultPriceItems().add((PriceItem) selectEvent.getObject());
    }

    /**
     * AJAX unselection (removal) handler for default {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param ignored
     */
    public void onDefaultPriceItemUnselect(UnselectEvent ignored) {
//        defaultPriceItem = null;
    }

    /**
     * AJAX selection (addition) handler for {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param selectEvent
     */
    public void onDefaultPriceItemSelect(SelectEvent selectEvent) {
/*
        if (conversationData.getDefaultPriceItem() != null) {
            // ignore
        }
        else {
            conversationData.setDefaultPriceItem((PriceItem) selectEvent.getObject());
        }
*/
    }


    public List<PriceItem> getDefaultPriceItems() {
        return conversationData.getDefaultPriceItems();
    }


    /**
     * NOOP, this is required for the PrimeFaces {@link org.primefaces.component.autocomplete.AutoComplete}, but the
     * actual setting of the default {@link PriceItem} is handled in the ajax event listener only
     * @param defaultPriceItems
     */
    public void setDefaultPriceItems(List<PriceItem> defaultPriceItems) {
        conversationData.setDefaultPriceItems(defaultPriceItems);
    }


    /**
     *
     * Used for {@link org.primefaces.component.autocomplete.AutoComplete}ing the default price item, restrict search
     * to only the currently selected {@link PriceItem}s.  If there is already a default price item, no results will
     * be returned to prevent an additional default price item from being set
     *
     * @param query
     * @return
     */
    public List<PriceItem> searchSelectedPriceItems(String query) {

/*
        if (defaultPriceItems != null && defaultPriceItems.size() > 0) {
            // don't offer anything if there is already a selected default price item
            return new ArrayList<PriceItem>();
        }
*/

        return priceListCache.searchPriceItems(getDefaultPriceItems(), query);
    }


    /**
     * Used to search all {@link PriceItem}s, but will filter out currently selected {@link PriceItem}s
     *
     * @param query
     * @return
     */
    public List<PriceItem> searchPriceItems(String query) {
        List<PriceItem> searchResults = priceListCache.searchPriceItems(query);
        // filter out price items that are already selected
        if (priceItems != null) {
            for (PriceItem priceItem : priceItems) {
                searchResults.remove(priceItem);
            }
        }

        return searchResults;
    }

    public PriceItem getDefaultPriceItem() {
        return defaultPriceItem;
    }

    public void setDefaultPriceItem(PriceItem defaultPriceItem) {
        this.defaultPriceItem = defaultPriceItem;
    }

    /**
     * Encapsulate logic for coming up with nice {@link PriceItem} labels that fit in the allotted space
     *
     * @param priceItem
     * @return
     */
    public String labelFor(PriceItem priceItem) {

        if (priceItem == null) {
            return "";
        }

        final int MAX_NAME = 45;

        if (priceItem.getName().length() > MAX_NAME){
            return priceItem.getName().substring(0, MAX_NAME) + "... (" + priceItem.getId() + ")";
        }
        else if (priceItem.getName().length() + priceItem.getPlatformName().length() < MAX_NAME) {
            return priceItem.getPlatformName() + ": " + priceItem.getName() + " (" + priceItem.getId() + ")";
        }
        return priceItem.getName() + " (" + priceItem.getId() + ")";
    }

}
