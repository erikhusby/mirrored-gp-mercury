package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductManager;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductSearcher;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * TODO: Update method documentation, especially around price item selection.
 */
@Named
@RequestScoped
public class ProductForm extends AbstractJsfBean {

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

    @Inject
    private ProductSearcher productSearcher;

    @Inject
    private FacesContext facesContext;


    /**
     * Transaction support for create / update operations
     */
    @Inject
    private ProductManager productManager;

    /**
     * Flag so we don't issue the same summary warning more than once per request
     */
    private boolean issuedSummaryMessageForPriceItemsNotOnCurrentPriceList;


    private static final String GLOBAL_MESSAGE_PRICE_ITEMS_NOT_ON_PRICE_LIST =
            "One or more price items associated with this product do not appear on the current quote server price list.  " +
            "These price items have been temporarily removed from this product; hit Save to remove these price items permanently or Cancel to abort.";


    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    private static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;
    private Product product;

    /**
     * These are in their own field since they are JAXB {@link PriceItem} DTOs and not JPA entities
     */
    private List<PriceItem> priceItems;

    /**
     * This is in its own field since this is a JAXB {@link PriceItem} DTO and not a JPA entity
     */
    private PriceItem defaultPriceItem;

    private List<Product> addOns;


    /**
     * Hook for the preRenderView event that initiates the long running conversation and sets up conversation scoped
     * data from the product, also initializes the form as appropriate
     */
    public void onPreRenderView() {
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
        return product == null || product.getProductId() == null;
    }


    private void issueMessagesForPriceItemNotOnPriceList(String clientId, String clientMessage) {

        // issue summary warning at most once
        if (! issuedSummaryMessageForPriceItemsNotOnCurrentPriceList) {
            // detail message not shown for global messages so set to null
            addErrorMessage(GLOBAL_MESSAGE_PRICE_ITEMS_NOT_ON_PRICE_LIST);
            issuedSummaryMessageForPriceItemsNotOnCurrentPriceList = true;
        }
        // need to keep the summary null for clientMessage or it ends up showing in the global message area
        addErrorMessage(clientId, null, clientMessage);
    }


    private String getClientMessageForPriceItemNotInPriceList(PriceItem priceItemDto, boolean isPrimary) {
        String message = "%s price item '%s: %s: %s' did not appear on the current quote server price list and has temporarily been removed from this product";
        message = String.format(
                message,
                isPrimary ? "Primary" : "Optional",
                priceItemDto.getPlatformName(), priceItemDto.getCategoryName(), priceItemDto.getName());

        return message;
    }


    /**
     * Initialize the form if this is not a postback
     */
    private void initForm() {
        if (!facesContext.isPostback()) {
            if (isCreating()) {
                // No form initialization needed for create
            } else {

                if (product.getPrimaryPriceItem() != null) {
                    PriceItem priceItemDto = entityToDto(product.getPrimaryPriceItem());

                    if (! priceListCache.contains(priceItemDto)) {
                        issueMessagesForPriceItemNotOnPriceList("defaultPriceItem", getClientMessageForPriceItemNotInPriceList(priceItemDto, true));
                        defaultPriceItem = null;
                    }
                    else {
                        defaultPriceItem = entityToDto(product.getPrimaryPriceItem());
                    }
                }
                if (product.getOptionalPriceItems() != null) {
                    priceItems = new ArrayList<PriceItem>();
                    for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : product.getOptionalPriceItems()) {

                        PriceItem priceItemDto = entityToDto(priceItem);
                        if (! priceListCache.contains(priceItemDto)) {
                            issueMessagesForPriceItemNotOnPriceList("priceItem", getClientMessageForPriceItemNotInPriceList(priceItemDto, false));
                            defaultPriceItem = null;
                        }
                        else {
                            priceItems.add(priceItemDto);
                        }
                    }
                }

                // TODO: is this needed? or does the actual backing model work for p:autoComplete?
                if (product.getAddOns() != null) {
                    addOns = new ArrayList<Product>();
                    addOns.addAll(product.getAddOns());
                }
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
                null, null, null, null, null, DEFAULT_TOP_LEVEL, DEFAULT_WORKFLOW_NAME, false);
    }

    /**
     * Enumerate the product families
     * @return
     */
    public List<ProductFamily> getProductFamilies() {
        List<ProductFamily> productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);

        return productFamilies;
    }


    public String save() {

        // need to calculate 'creating' here, doing it after writing out the entity is too late (we always get 'updating')
        boolean creating = isCreating();

        try {
            addAllAddOnsToProduct();
            addAllPriceItemsToProduct();

            // If there are duplicate price items, send an error message
            String[] duplicatePriceItems = product.getDuplicatePriceItemNames();
            if (duplicatePriceItems != null) {
                addErrorMessage("Cannot save with duplicate price items: " + StringUtils.join(duplicatePriceItems, ", "));
                return null;
            }

            productManager.save(product);
        }
        catch (Exception e ) {
            addErrorMessage(e.getMessage());
            return null;
        }

        addInfoMessage("Product \"" + product.getProductName() + "\" has been " + (creating ? "created." : "updated."));
        return redirect("view") + addProductParam();
    }


    private String addProductParam() {
        return "&product=" + product.getBusinessKey();
    }


    /**
     * Entify all the addons from our JAXB DTOs and add them to the {@link Product} before persisting
     */
    private void addAllAddOnsToProduct() {
        Date now = Calendar.getInstance().getTime();
        product.getAddOns().clear();
        if ( addOns != null) {
            for ( Product aProductAddOn : addOns ) {
                if ( aProductAddOn != null ) {
                    if ( aProductAddOn.isAvailable() || aProductAddOn.getAvailabilityDate().after( now ) ) {
                        product.addAddOn(aProductAddOn);
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
    private void addAllPriceItemsToProduct() throws ApplicationValidationException {

        if (defaultPriceItem == null) {
            // ApplicationValidationException is rollback=true, but we're not in a transaction at the time of this
            // validation, I just wanted to reuse the same exception type since this is an application validation
            throw new ApplicationValidationException("Default price item must be entered");
        }

        product.setPrimaryPriceItem(findEntity(defaultPriceItem));

        product.getOptionalPriceItems().clear();
        if (priceItems != null) {
            for (PriceItem priceItem : priceItems) {
                org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity = findEntity(priceItem);
                product.addPriceItem(entity);
            }
        }
    }

    public Product getProduct() {
        return product;
    }
    public void setProduct(final Product product) {
        this.product = product;
    }

    public Integer getExpectedCycleTimeDays() {
        return convertCycleTimeSecondsToDays(product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        product.setExpectedCycleTimeSeconds(convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        return convertCycleTimeSecondsToDays(product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        product.setGuaranteedCycleTimeSeconds(convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
    }


    public List<Product> getAddOns() {
        if (product == null) {
            return new ArrayList<Product>();
        }

        if (addOns == null) {
            addOns = new ArrayList<Product>(product.getAddOns());
            Collections.sort(addOns);
        }
        return addOns;
    }


    public void setAddOns(final List<Product> addOns) {
        this.addOns = addOns;
    }


    public List<PriceItem> getPriceItems() {
        return priceItems;
    }


    public void setPriceItems(List<PriceItem> priceItems) {
        this.priceItems = priceItems;
    }


    /**
     * Converts cycle times from days to seconds.
     * @return the number of seconds.
     */
    public static Integer convertCycleTimeDaysToSeconds(Integer cycleTimeDays) {
        Integer cycleTimeSeconds = null;
        if ( cycleTimeDays != null ) {
            cycleTimeSeconds = ( cycleTimeDays == null ? 0 : cycleTimeDays.intValue() * ONE_DAY_IN_SECONDS);
        }
        return cycleTimeSeconds;
    }

    /**
     * Converts cycle times from seconds to days.
     * This method rounds down to the nearest day
     * @param cycleTimeSeconds
     * @return the number of days.
     */
    public static Integer convertCycleTimeSecondsToDays(Integer cycleTimeSeconds) {
        Integer cycleTimeDays = null;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_DAY_IN_SECONDS) {
            cycleTimeDays =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_DAY_IN_SECONDS)) / ONE_DAY_IN_SECONDS;
        }
        return cycleTimeDays;
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
        return priceListCache.searchPriceItems(query);
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
        searchResults.remove(defaultPriceItem);

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
            return priceItem.getName().substring(0, MAX_NAME) + "... ";
        }
        else if (priceItem.getPlatformName().length() + priceItem.getName().length() < MAX_NAME) {
            return priceItem.getPlatformName() + ": " + priceItem.getName();
        }
        return priceItem.getName();
    }

    public String addOnLabel(Product product) {

        if ((product == null) || (product.getProductName() == null)) {
            return "";
        }

        final int MAX_NAME = 45;

        if (product.getProductName().length() > MAX_NAME){
            return product.getProductName().substring(0, MAX_NAME) + "... ";
        } else if ( product.getProductName().length() + product.getPartNumber().length() < MAX_NAME ){
            return product.getProductName() + " : " + product.getPartNumber();
        }
        return product.getProductName();
    }


    public List<Product> searchProductsForAddonsInProductEdit(String searchText) {
        return productSearcher.searchProductsForAddonsInProductEdit(product, searchText);
    }

}