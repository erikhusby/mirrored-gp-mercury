package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.presentation.products.ProductActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.sap.services.SAPIntegrationException;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

@Stateful
@RequestScoped
/**
 * Transactional manager for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s.
 */
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ProductEjb {

    private static final Log log = LogFactory.getLog(ProductActionBean.class);

    private ProductDao productDao;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ProductEjb() {
    }

    private AttributeArchetypeDao attributeArchetypeDao;

    private AuditReaderDao auditReaderDao;

    private SapIntegrationService sapService;

    private SAPAccessControlEjb accessController;

    private SAPProductPriceCache productPriceCache;

    @Inject
    public ProductEjb(ProductDao productDao, SapIntegrationService sapService, AuditReaderDao auditReaderDao,
                      AttributeArchetypeDao attributeArchetypeDao, SAPAccessControlEjb accessController,
                      SAPProductPriceCache productPriceCache) {
        this.productDao = productDao;
        this.attributeArchetypeDao = attributeArchetypeDao;
        this.auditReaderDao = auditReaderDao;
        this.sapService = sapService;
        this.accessController = accessController;
        this.productPriceCache = productPriceCache;
    }

    /**
     * Using the product here because I need all the edited fields for both save and edit.
     *
     * @param product             The product with all updated date (it was found for edit).
     * @param addOnTokenInput     The add ons
     * @param priceItemTokenInput The price items
     * @param allLengthsMatch     Do the lengths match
     * @param criteria            The risk criteria
     * @param operators           The operators
     * @param values              The values
     * @param genotypingChipInfo  Genotyping chips for this product
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void saveProduct(
            Product product, ProductTokenInput addOnTokenInput, PriceItemTokenInput priceItemTokenInput,
            boolean allLengthsMatch, String[] criteria, String[] operators, String[] values,
            List<Triple<String, String, String>> genotypingChipInfo, PriceItemTokenInput externalPriceItemTokenInput) {

        populateTokenListFields(product, addOnTokenInput, priceItemTokenInput, externalPriceItemTokenInput);
        // If all lengths match, just send it.
        if (allLengthsMatch) {
            product.updateRiskCriteria(criteria, operators, values);
        } else {
            // Otherwise, there must be a boolean and we need to make them synchronized.
            String[] fullOperators = new String[criteria.length];
            String[] fullValues = new String[criteria.length];

            // Insert the operators and values for booleans, otherwise, use the next item.
            int fullPosition = 0;
            int originalPosition = 0;
            for (String criterion : criteria) {
                RiskCriterion.RiskCriteriaType type = RiskCriterion.RiskCriteriaType.findByLabel(criterion);
                if (type.getOperatorType() == Operator.OperatorType.BOOLEAN) {
                    fullOperators[fullPosition] = type.getOperators().get(0).getLabel();
                    fullValues[fullPosition] = "true";
                } else {
                    fullOperators[fullPosition] = operators[originalPosition];
                    fullValues[fullPosition] = values[originalPosition];

                    // Only increment original position for values that are not boolean.
                    originalPosition++;
                }

                // Always increment full position.
                fullPosition++;
            }

            product.updateRiskCriteria(criteria, fullOperators, fullValues);
        }

        // Need to persist for product
        productDao.persist(product);

        // Genotyping chip mappings are AttributeArchetypes which are persisted independent of Product.
        persistGenotypingChipMappings(product.getPartNumber(), genotypingChipInfo);
    }

    private void populateTokenListFields(Product product, ProductTokenInput addOnTokenInput,
                                         PriceItemTokenInput priceItemTokenInput,
                                         PriceItemTokenInput externalPriceItemTokenInput) {
        product.getAddOns().clear();
        product.getAddOns().addAll(addOnTokenInput.getTokenObjects());
        product.setPrimaryPriceItem(priceItemTokenInput.getItem());
        product.setExternalPriceItem(externalPriceItemTokenInput.getItem());
    }


    /**
     * Returns the currently defined but not necessarily mapped chip families and their chip names
     * which are eligible for mapping to a product. This must also include all mapped chips too.
     */
    public Map<String, SortedSet<String>> findChipFamiliesAndNames() {
        Map<String, SortedSet<String>> map = new HashMap<>();
        for (GenotypingChip chip : attributeArchetypeDao.findGenotypingChips()) {
            SortedSet<String> names = map.get(chip.getChipTechnology());
            if (names == null) {
                names = new TreeSet<>();
                map.put(chip.getChipTechnology(), names);
            }
            names.add(chip.getChipName());
        }
        return map;
    }


    /**
     * Looks up a genotyping chip mapping that was active on the effective date.
     *
     * @return (chip family, chip name) or (null, null) if no match was found.
     */
    public Pair<String, String> getGenotypingChip(ProductOrder productOrder, Date effectiveDate) {
        if (productOrder.getProduct() != null) {
            String productPartNumber = productOrder.getProduct().getPartNumber();
            String productOrderName = productOrder.getName();
            return getGenotypingChip(productPartNumber, productOrderName, effectiveDate);
        } else {
            return Pair.of(null, null);
        }
    }

    /**
     * Looks up a genotyping chip mapping that was active on the effective date.
     *
     * @return (chip family, chip name) or (null, null) if no match was found.
     */
    public Pair<String, String> getGenotypingChip(String productPartNumber, String productOrderName,
                                                  Date effectiveDate) {

        // Retrieves the historical chip mappings, one mapping for each product and pdo substring.
        Collection<GenotypingChipMapping> mappings = attributeArchetypeDao.getMappingsAsOf(effectiveDate);

        // Finds the best match for the product part number and pdo name.
        // The map is sorted in search order, the same order as displayed in UI for Product chip mappings.
        SortedMap<String, GenotypingChipMapping> partialMatches = partNumberMatches(productPartNumber, mappings);
        GenotypingChipMapping bestMatch = null;
        for (Map.Entry<String, GenotypingChipMapping> entry : partialMatches.entrySet()) {
            if (productOrderName.contains(entry.getValue().getPdoSubstring())) {
                bestMatch = partialMatches.get(entry.getKey());
                break;
            }
        }
        return (bestMatch != null) ?
                Pair.of(bestMatch.getChipFamily(), bestMatch.getChipName()) : Pair.of((String) null, (String) null);
    }

    /**
     * Returns the product configuration genotyping chip mappings that match on the part number,
     * ordered by decreasing pdoString, nulls last.
     *
     * @param productPartNumber part number to match
     * @param allChips          collection of archetypes and may include other namespaces and groups.
     *
     * @return map of (product part number + delimiter + pdoString) -> archetype
     */
    private SortedMap<String, GenotypingChipMapping> partNumberMatches(
            String productPartNumber, Collection<GenotypingChipMapping> allChips) {

        SortedMap<String, GenotypingChipMapping> matches = new TreeMap<>(Collections.reverseOrder());

        for (GenotypingChipMapping chip : allChips) {
            if (productPartNumber.equals(chip.getProductPartNumber())) {
                matches.put(chip.getArchetypeName(), chip);
            }
        }
        return matches;
    }

    /**
     * Updates, deletes, adds the mappings for a product part number to genotyping chips.
     * @param productPartNumber  the product part number to be mapped
     * @param genotypingChipInfo (Chip family, chip name, PDO name substring) to be mapped
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistGenotypingChipMappings(String productPartNumber,
                                              List<Triple<String, String, String>> genotypingChipInfo) {
        final Date now = new Date();
        SortedMap<String, GenotypingChipMapping> currentMappings = partNumberMatches(productPartNumber,
                attributeArchetypeDao.getMappingsAsOf(now));

        for (Triple<String, String, String> familyAndNameAndPdoSubstring : genotypingChipInfo) {
            String chipFamily = familyAndNameAndPdoSubstring.getLeft();
            String chipName = familyAndNameAndPdoSubstring.getMiddle();
            String pdoSubstring = familyAndNameAndPdoSubstring.getRight();

            String mappingName = productPartNumber + (StringUtils.isNotBlank(pdoSubstring) ?
                    GenotypingChipMapping.DELIMITER + pdoSubstring : "");

            // If no mapping exists for this product + pdo substring, it is created.
            // If a mapping exists for this product + pdo substring, and the target chip is unchanged, it is
            // kept as-is.
            // If a mapping exists but with a new target chip, the mapping get inactivated and a new mapping
            // gets created so that an old genotyping run will still use the mapping that was in effect
            // when the run occurred.
            GenotypingChipMapping currentMapping = currentMappings.get(mappingName);

            if (currentMapping == null || !currentMapping.getChipFamily().equals(chipFamily) ||
                !currentMapping.getChipName().equals(chipName)) {

                attributeArchetypeDao.persist(new GenotypingChipMapping(mappingName, chipFamily, chipName, now));
            } else {
                // Removed from the collection so it doesn't get inactivated.
                currentMappings.remove(mappingName);
            }
        }

        // Inactivates existing mappings which were changed or deleted.
        for (GenotypingChipMapping mapping : currentMappings.values()) {
            mapping.setInactiveDate(now);
        }
    }

    /**
     * Returns info on the genotyping chips mapped currently mapped to the given product. Multiple mappings are
     * distinguished from one another using pdoSubstring.
     *
     * @param productPartNumber is used to determine the returned info. Null returns no mappings.
     *
     * @return List of chip family, chip name, pdo substring.  These are ordered the same way they
     * are used to lookup a mapping: by decreasing pdo substring with nulls last.
     */
    public List<Triple<String, String, String>> getCurrentMappedGenotypingChips(String productPartNumber) {
        List<Triple<String, String, String>> chipInfo = new ArrayList<>();
        if (productPartNumber != null) {
            for (GenotypingChipMapping mapping : attributeArchetypeDao.getMappingsAsOf(new Date())) {
                if (productPartNumber.equals(mapping.getProductPartNumber())) {
                    chipInfo.add(Triple.of(mapping.getChipFamily(), mapping.getChipName(),
                            StringUtils.trimToNull(mapping.getPdoSubstring())));
                }
            }
        }
        Collections.sort(chipInfo, new Comparator<Triple<String, String, String>>() {
            @Override
            public int compare(Triple<String, String, String> o1, Triple<String, String, String> o2) {
                int pdoSubstringComparison = (o2.getRight() == null) ? (o1.getRight() == null ? 0 : -1) :
                        (o1.getRight() == null) ? 1 : o2.getRight().compareTo(o1.getRight());
                if (pdoSubstringComparison != 0) {
                    return pdoSubstringComparison;
                }
                int familyComparison = o1.getLeft().compareTo(o2.getLeft());
                if (familyComparison != 0) {
                    return familyComparison;
                }
                int nameComparison = o1.getMiddle().compareTo(o2.getMiddle());
                return nameComparison;
            }
        });
        return chipInfo;
    }

    /**
     * This method has the responsibility to take the given product and attempt to publish it to SAP
     * @param productToPublish A product which needs to have its information either created or updated in SAP
     * @throws SAPIntegrationException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void publishProductToSAP(Product productToPublish) throws SAPIntegrationException {
        publishProductToSAP(productToPublish, true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
    }

    /**
     * This method has the responsibility to take the given product and attempt to publish it to SAP
     * @param productToPublish A product which needs to have its information either created or updated in SAP
     * @param extendProductsToOtherPlatforms
     * @param publishType
     * @throws SAPIntegrationException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void publishProductToSAP(Product productToPublish, boolean extendProductsToOtherPlatforms,
                                    SapIntegrationService.PublishType publishType) throws SAPIntegrationException {
        try {
            sapService.publishProductInSAP(productToPublish, extendProductsToOtherPlatforms, publishType);
            productToPublish.setSavedInSAP(true);

        } catch (SAPIntegrationException e) {
            throw new SAPIntegrationException(e.getMessage());
        }
    }

    /**
     * This method has the responsibility of taking the products passed to it and attempting to publish them to SAP.
     * @param productsToPublish a collection of products which needs to have their information either created or
     *                          updated in SAP
     * @throws SAPIntegrationException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void publishProductsToSAP(Collection<Product> productsToPublish) throws ValidationException {
        publishProductsToSAP(productsToPublish, true, SapIntegrationService.PublishType.CREATE_AND_UPDATE);
    }

    /**
     * This method has the responsibility of taking the products passed to it and attempting to publish them to SAP.
     * @param productsToPublish a collection of products which needs to have their information either created or
     *                          updated in SAP
     * @param extendProductsToOtherPlatforms
     * @param publishType
     * @throws SAPIntegrationException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void publishProductsToSAP(Collection<Product> productsToPublish, boolean extendProductsToOtherPlatforms,
                                      SapIntegrationService.PublishType publishType) throws ValidationException {
        List<String> errorMessages = new ArrayList<>();
        for (Product productToPublish : productsToPublish) {
            try {
                publishProductToSAP(productToPublish, extendProductsToOtherPlatforms, publishType);
            } catch (SAPIntegrationException e) {
                errorMessages.add(productToPublish.getPartNumber() + ": " + e.getMessage());
                log.error(e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(errorMessages)) {
            throw new ValidationException("Some errors were found pushing products", errorMessages);
        }
        productPriceCache.refreshCache();
    }
}
