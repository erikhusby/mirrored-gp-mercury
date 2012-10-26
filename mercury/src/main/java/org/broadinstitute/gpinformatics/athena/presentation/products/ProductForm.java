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
    private FacesContext facesContext;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    public static final int ONE_HOUR_IN_SECONDS = 3600;
    private Product product;

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
            if  ((product.getPartNumber() != null) && !StringUtils.isBlank(product.getPartNumber())) {
                product = productDao.findByBusinessKey(product.getPartNumber());
                addOns.addAll( product.getAddOns() );
            }
        }
    }

    /**
     * Initialze an empty {@link Product} so fields can drill into the backing product without NPEs
     */
    public void initEmptyProduct() {
        product = new Product(null, null, null, null, null, null, null,
                null, null, null, null, null, DEFAULT_TOP_LEVEL, DEFAULT_WORKFLOW_NAME, false);
        addOns = new ArrayList<Product>();
    }

    /**
     * Enumerate the product families
     * @return
     */
    public List<ProductFamily> getProductFamilies() {
        return  productFamilyDao.findAll();
    }


    public String save() {
        if (! dateRangeOkay()) {
            String errorMessage = "Availability date must precede discontinued date.";
            addErrorMessage("Date range invalid", errorMessage, errorMessage );
            return "create";
        }
        if (getProduct().getProductId() == null ) {
            return create();
        } else {
            return edit();
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

        for (PriceItem priceItem : conversationData.getPriceItems()) {

            org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity = findEntity(priceItem);
            product.addPriceItem(entity);

            if (conversationData.getDefaultPriceItem().equals(priceItem)) {
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
        return conversationData.getPriceItems();
    }

    /**
     * NOOP, this is required for the PrimeFaces {@link org.primefaces.component.autocomplete.AutoComplete}, but the
     * actual setting of {@link PriceItem}s is handled in the ajax event listener only
     * @param ignored
     */
    public void setPriceItems(List<PriceItem> ignored) {
        // noop
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
        conversationData.removePriceItem(priceItem);

        // nuke out the default price item if it was the same price item we just removed
        if (conversationData.getDefaultPriceItem() != null && conversationData.getDefaultPriceItem().equals(priceItem)) {
            conversationData.setDefaultPriceItem(null);
        }
    }

    /**
     * AJAX selection (addition) handler for default {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param selectEvent
     */

    public void onPriceItemSelect(SelectEvent selectEvent) {
        conversationData.addPriceItem((PriceItem) selectEvent.getObject());
    }

    /**
     * AJAX unselection (removal) handler for default {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param ignored
     */
    public void onDefaultPriceItemUnselect(UnselectEvent ignored) {
        conversationData.setDefaultPriceItem(null);
    }

    /**
     * AJAX selection (addition) handler for {@link PriceItem} for the PrimeFaces
     * {@link org.primefaces.component.autocomplete.AutoComplete}
     *
     * @param selectEvent
     */
    public void onDefaultPriceItemSelect(SelectEvent selectEvent) {
        if (conversationData.getDefaultPriceItem() != null) {
            // ignore
        }
        else {
            conversationData.setDefaultPriceItem((PriceItem) selectEvent.getObject());
        }
    }


    public List<PriceItem> getDefaultPriceItems() {
        return conversationData.getDefaultPriceItems();
    }


    /**
     * NOOP, this is required for the PrimeFaces {@link org.primefaces.component.autocomplete.AutoComplete}, but the
     * actual setting of the default {@link PriceItem} is handled in the ajax event listener only
     * @param ignored
     */
    public void setDefaultPriceItems(List<PriceItem> ignored) {
        // noop, this is handled in the ajax event listener only!
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

        if (conversationData.getDefaultPriceItems() != null && conversationData.getDefaultPriceItems().size() > 0) {
            // don't offer anything if there is already a selected default price item
            return new ArrayList<PriceItem>();
        }

        return priceListCache.searchPriceItems(conversationData.getPriceItems(), query);
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
        for (PriceItem priceItem : getPriceItems()) {
            searchResults.remove(priceItem);
        }

        return searchResults;
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
