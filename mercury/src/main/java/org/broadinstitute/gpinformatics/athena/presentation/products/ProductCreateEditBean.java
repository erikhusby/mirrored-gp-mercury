package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductSearcher;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ManagedBean
@ViewScoped
public class ProductCreateEditBean extends AbstractJsfBean implements Serializable {

    @Inject
    private ProductFamilyDao productFamilyDao;

    /**
     * Source of quote server sourced price data
     */
    @Inject
    private PriceListCache priceListCache;

    @Inject
    private ProductSearcher productSearcher;

    @Inject
    private BSPMaterialTypeList materialTypeListCache;

    /**
     * Transaction support for create / update operations
     */
    @Inject
    private ProductEjb productEjb;

    /**
     * Flag so we don't issue the same summary warning more than once per request
     */
    private boolean issuedSummaryMessageForPriceItemsNotOnCurrentPriceList;


    private static final String GLOBAL_MESSAGE_PRICE_ITEMS_NOT_ON_PRICE_LIST =
            "One or more price items associated with this product do not appear on the current quote server price list.  " +
            "These price items have been temporarily removed from this product; hit Save to remove these price items permanently or Cancel to abort.";


    private Product product;

    /**
     * GPLIM-559 The part number input field is bound to this separate part number property to avoid issues with updated
     * model values being cycled back out to the viewParam after application validation failures in #save().  If we
     * pass application validation, this part number is passed into the ProductManager#save method to undergo further
     * validations.  If all these validations pass, the part number is copied into the model object and saved to the db.
     * This is the general JSF pattern that should be followed for user-editable business keys.
     */
    private String partNumber;

    /**
     * These are in their own field since they are JAXB {@link PriceItem} DTOs and not JPA entities
     */
    private List<PriceItem> optionalPriceItems;

    /**
     * This is in its own field since this is a JAXB {@link PriceItem} DTO and not a JPA entity
     */
    private PriceItem primaryPriceItem;

    /**
     * This is a {@link List} to support p:autoComplete
     */
    private List<Product> addOns;

    /**
     * These are in their own field since they are JAXB {@link org.broadinstitute.bsp.client.sample.MaterialType} DTOs and not JPA entities
     */
    private List<org.broadinstitute.bsp.client.sample.MaterialType> allowedMaterialTypes;



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
    public void onPreRenderView() {
        if (!FacesContext.getCurrentInstance().isPostback()) {

            if (isCreating()) {
                product = Product.makeEmptyProduct();
            } else {

                if (product.getPrimaryPriceItem() != null) {
                    PriceItem priceItemDto = entityToDto(product.getPrimaryPriceItem());

                    if (!priceListCache.contains(priceItemDto)) {
                        issueMessagesForPriceItemNotOnPriceList("defaultPriceItem", getClientMessageForPriceItemNotInPriceList(priceItemDto, true));
                        primaryPriceItem = null;
                    } else {
                        primaryPriceItem = entityToDto(product.getPrimaryPriceItem());
                    }
                }

                optionalPriceItems = new ArrayList<PriceItem>();
                for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : product.getOptionalPriceItems()) {

                    PriceItem priceItemDto = entityToDto(priceItem);
                    if (!priceListCache.contains(priceItemDto)) {
                        issueMessagesForPriceItemNotOnPriceList("priceItem", getClientMessageForPriceItemNotInPriceList(priceItemDto, false));
                        primaryPriceItem = null;
                    } else {
                        optionalPriceItems.add(priceItemDto);
                    }
                }

                addOns = new ArrayList<Product>();
                addOns.addAll(product.getAddOns());

                partNumber = product.getPartNumber();
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

    private MaterialType entityToDto(org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType entity) {
        return new org.broadinstitute.bsp.client.sample.MaterialType(entity.getFullName() );
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
            productEjb.save(product, partNumber, addOns, primaryPriceItem, optionalPriceItems, allowedMaterialTypes);

        } catch (ProductEjb.ExpiredAddOnsException e) {
            addErrorMessage("Add-ons are no longer available: " + e.getMessage());
            return null;
        } catch (ProductEjb.DuplicateBusinessKeyException e) {
            addErrorMessage("Part number already in use by another Product: " + partNumber);
            return null;
        } catch (ProductEjb.NoPrimaryPriceItemException e) {
            addErrorMessage("No Primary Price Item set");
            return null;
        } catch (ProductEjb.IncompatibleDatesException e) {
            addErrorMessage("Incompatible availablility and discontinued dates, discontinued date must be defined and come after availability date");
            return null;
        } catch (ProductEjb.DuplicatePriceItemNamesException e) {
            addErrorMessage("Cannot save with duplicate price items: " + e.getMessage());
            return null;
        } catch (RuntimeException e) {
            addErrorMessage("Error creating Product: " + e);
            return null;
        }

        addInfoMessage("Product \"" + product.getProductName() + "\" has been " + (creating ? "created." : "updated."));
        return redirect("view") + addProductParam();
    }


    private String addProductParam() {
        return "&product=" + product.getBusinessKey();
    }


    public Product getProduct() {
        return product;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public Integer getExpectedCycleTimeDays() {
        return Product.convertCycleTimeSecondsToDays(product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        product.setExpectedCycleTimeSeconds(Product.convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        return Product.convertCycleTimeSecondsToDays(product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        product.setGuaranteedCycleTimeSeconds(Product.convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
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


    public List<PriceItem> getOptionalPriceItems() {
        if (product == null) {
            return new ArrayList<PriceItem>();
        }

        if (optionalPriceItems == null) {
            optionalPriceItems = new ArrayList<PriceItem>();

            for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : product.getOptionalPriceItems()) {
                optionalPriceItems.add(entityToDto(priceItem));
            }
        }
        return optionalPriceItems;
    }


    public void setOptionalPriceItems(List<PriceItem> optionalPriceItems) {
        this.optionalPriceItems = optionalPriceItems;
    }


    public List<MaterialType> getAllowedMaterialTypes() {
        if (product == null) {
            return new ArrayList<MaterialType>();
        }

        if ( allowedMaterialTypes == null ) {
            allowedMaterialTypes = new ArrayList<MaterialType>();
            for ( org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType materialType : product.getAllowableMaterialTypes() ) {
                allowedMaterialTypes.add( entityToDto(materialType) );
            }
        }
        return allowedMaterialTypes;
    }

    public void setAllowedMaterialTypes(List<MaterialType> allowedMaterialTypes) {
        this.allowedMaterialTypes = allowedMaterialTypes;
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
        if (optionalPriceItems != null) {
            for (PriceItem priceItem : optionalPriceItems) {
                searchResults.remove(priceItem);
            }
        }
        searchResults.remove(primaryPriceItem);

        return searchResults;
    }

    public List<MaterialType> searchMaterialTypes(String query) {
        List<MaterialType> searchResults = materialTypeListCache.find(query);
        // filter out material types that are already selected
        if (allowedMaterialTypes != null) {
            for (MaterialType materialType : allowedMaterialTypes) {
                searchResults.remove(materialType);
            }
        }

        return searchResults;
    }

    public PriceItem getPrimaryPriceItem() {
        return primaryPriceItem;
    }

    public void setPrimaryPriceItem(PriceItem primaryPriceItem) {
        this.primaryPriceItem = primaryPriceItem;
    }

    /**
     * Encapsulate logic for coming up with nice {@link PriceItem} labels that fit in the allotted space.  It would be
     * better to make sure the PrimeFaces p:autoComplete fields were wide enough to accommodate our PriceItem text,
     * but the multiple=true p:autoComplete does not seem to honor size=xxx specifications.
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

    public String labelForMaterialType(MaterialType materialType) {

        if (materialType == null) {
            return "";
        }

        final int MAX_NAME = 45;

        if (materialType.getName().length() > MAX_NAME){
            return materialType.getName().substring(0, MAX_NAME) + "... ";
        }
        else if (materialType.getCategory().length() + materialType.getName().length() + 2 < MAX_NAME) {
            return materialType.getCategory() + ": " + materialType.getName();
        }
        return materialType.getName();
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
        return productSearcher.searchProductsForAddonsInProductEdit(getProduct(), searchText);
    }

}