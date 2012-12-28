package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.samples.MaterialTypeDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Boundary bean for Products
 */
@Stateful
@RequestScoped
public class ProductEjb {

    public static class IncompatibleDatesException extends Exception {}

    public static class ExpiredAddOnsException extends Exception {
        public ExpiredAddOnsException(String s) {
            super(s);
        }
    }

    public static class DuplicateBusinessKeyException extends Exception {}

    public static class NoPrimaryPriceItemException extends Exception {}

    /**
     * Note this exception looks for duplicate price item <b>names</b>, and not duplicated <b>triplets</b>, the
     * natural compound keys of price items.  With the current billing done by price item name-keyed spreadsheet columns
     * we cannot have colliding price item names.  This restriction can hopefully be relaxed once automated billing
     * is in place.
     */
    public static class DuplicatePriceItemNamesException extends Exception {
        public DuplicatePriceItemNamesException(String s) {
            super(s);
        }
    }

    private Log log = LogFactory.getLog(ProductEjb.class);

    @Inject
    private ProductDao productDao;


    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private MaterialTypeDao materialTypeDao;

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
      * Utility method to map JAXB DTOs to entities for material types
      * @param materialTypeDto
      * @return
      */
     private org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType dtoToEntity(
             org.broadinstitute.bsp.client.sample.MaterialType materialTypeDto) {

         org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType materialTypeEntity =
                 new org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType(
                         materialTypeDto.getCategory(), materialTypeDto.getName());
         materialTypeEntity.setFullName( materialTypeDto.getFullName() );

         return materialTypeEntity;
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
     * Utility method to grab a persistent/detached JPA entity corresponding to this JAXB DTO if one exists,
     * otherwise return just a transient JPA entity
     *
     * @param materialType
     * @return
     */
    private org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType findEntity(MaterialType materialType) {
        org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType entity =
                materialTypeDao.find(materialType.getCategory(), materialType.getName());

        if (entity == null) {
            entity = dtoToEntity(materialType);
        }

        return entity;
    }

    /**
     * Make sure part number is not duplicated
     *
     *
     * @param product
     * @param partNumber
     * @throws ApplicationValidationException
     */
    private void validateUniquePartNumber(Product product, String partNumber) throws DuplicateBusinessKeyException {
        Product existingProduct = productDao.findByPartNumber(partNumber);
        if (existingProduct != null && ! existingProduct.getProductId().equals(product.getProductId())) {
            throw new DuplicateBusinessKeyException();
        }
    }

    /**
     * Sanity check dates
     * @return
     */
    private void validateDateRangeOkay(Product product) throws IncompatibleDatesException {

        if ((product.getAvailabilityDate() != null ) &&
                (product.getDiscontinuedDate() != null ) &&
                (product.getAvailabilityDate().after(product.getDiscontinuedDate()))) {
            throw new IncompatibleDatesException();
        }
    }


    /**
     * validate the add-ons are still available
     */
    private void validateAddOnAvailability(List<Product> addOns) throws ProductEjb.ExpiredAddOnsException {
        if (addOns == null) {
            return;
        }

        for (Product aProductAddOn : addOns) {
            if (!aProductAddOn.isAvailable()) {
                throw new ProductEjb.ExpiredAddOnsException(aProductAddOn.getBusinessKey());
            }
        }
    }


    /**
     * Validate uniqueness of PriceItem <b>names</b> across the primary product and any add-ons.
     *
     * @param primaryPriceItem
     * @param optionalPriceItems
     * @param addOns
     *
     * @throws DuplicatePriceItemNamesException
     */
    private void validatePriceItemNameUniqueness(PriceItem primaryPriceItem, List<PriceItem> optionalPriceItems, List<Product> addOns) throws DuplicatePriceItemNamesException {
        Set<String> duplicatedPriceItemNames = new HashSet<String>();
        Set<String> priceItemNames = new HashSet<String>();

        priceItemNames.add(primaryPriceItem.getName());

        if (optionalPriceItems != null) {
            for (PriceItem priceItem : optionalPriceItems) {
                if (!priceItemNames.add(priceItem.getName())) {
                    duplicatedPriceItemNames.add(priceItem.getName());
                }
            }
        }

        if (addOns != null) {
            for (Product addOn : addOns) {
                if (!priceItemNames.add(addOn.getPrimaryPriceItem().getName())) {
                    duplicatedPriceItemNames.add(addOn.getPrimaryPriceItem().getName());
                }

                for (org.broadinstitute.gpinformatics.athena.entity.products.PriceItem priceItem : addOn.getOptionalPriceItems()) {
                    if (!priceItemNames.add(priceItem.getName())) {
                        duplicatedPriceItemNames.add(priceItem.getName());
                    }
                }
            }
        }

        if (! duplicatedPriceItemNames.isEmpty()) {
            throw new DuplicatePriceItemNamesException(StringUtils.join(duplicatedPriceItemNames.toArray()));
        }
    }


    public void save(Product product, String partNumber, List<Product> addOns, PriceItem primaryPriceItem,
                     List<PriceItem> optionalPriceItems, List<MaterialType> allowedMaterialTypes)
            throws ExpiredAddOnsException, DuplicateBusinessKeyException, IncompatibleDatesException, NoPrimaryPriceItemException, DuplicatePriceItemNamesException {

        validateUniquePartNumber(product, partNumber);
        validateDateRangeOkay(product);
        validateAddOnAvailability(addOns);

        if (primaryPriceItem == null) {
            throw new NoPrimaryPriceItemException();
        }

        validatePriceItemNameUniqueness(primaryPriceItem, optionalPriceItems, addOns);

        product.getAddOns().clear();
        if (addOns != null) {
           product.getAddOns().addAll(addOns);
        }

        product.setPrimaryPriceItem(findEntity(primaryPriceItem));

        product.getOptionalPriceItems().clear();
        if (optionalPriceItems != null) {
            for (PriceItem priceItem : optionalPriceItems) {
                org.broadinstitute.gpinformatics.athena.entity.products.PriceItem entity = findEntity(priceItem);
                product.addPriceItem(entity);
            }
        }

        product.getAllowableMaterialTypes().clear();
        if (allowedMaterialTypes != null) {
            for (MaterialType materialType : allowedMaterialTypes) {
                org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType entity = findEntity(materialType);
                product.addAllowableMaterialType(entity);
            }
        }

        // copy in the part number as the last thing we do before writing to db GPLIM-559
        product.setPartNumber(partNumber);

        // if we are doing a create we will need to persist and flush, otherwise just falling off the end of this
        // @Stateful method will commit our transaction
        if (product.getProductId() == null) {
            productDao.persist(product);
            productDao.flush();
        }
    }


}
