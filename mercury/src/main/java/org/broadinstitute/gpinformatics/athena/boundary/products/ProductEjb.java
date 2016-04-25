package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

@Stateful
@RequestScoped
/**
 * Transactional manager for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s.
 */
public class ProductEjb {
    private final ProductDao productDao;
    public final static String DELIMITER = " ";
    public final static String namespace = Product.class.getCanonicalName();
    public final static String group = Product.GENOTYPING_CHIP_CONFIG;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public ProductEjb() {
        this(null);
    }

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private AuditReaderDao auditReaderDao;

    @Inject
    public ProductEjb(ProductDao productDao) {
        this.productDao = productDao;
    }

    /**
     * Using the product here because I need all the edited fields for both save and edit.
     * @param product The product with all updated date (it was found for edit).
     * @param addOnTokenInput The add ons
     * @param priceItemTokenInput The price items
     * @param allLengthsMatch Do the lengths match
     * @param criteria The risk criteria
     * @param operators The operators
     * @param values The values
     * @param genotypingChipTechnologies Genotyping chip mapped to this product part number
     * @param genotypingChipNames Genotyping chip mapped to this product part number
     * @param genotypingChipPdoSubstrings Genotyping chip mapped to this product part number
     */
    public void saveProduct(
            Product product, ProductTokenInput addOnTokenInput, PriceItemTokenInput priceItemTokenInput,
            boolean allLengthsMatch, String[] criteria, String[] operators, String[] values,
            String[] genotypingChipTechnologies, String[] genotypingChipNames, String[] genotypingChipPdoSubstrings) {

        populateTokenListFields(product, addOnTokenInput, priceItemTokenInput);
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
        persistGenotypingChipMappings(product.getPartNumber(), genotypingChipTechnologies, genotypingChipNames,
                genotypingChipPdoSubstrings);
    }

    private void populateTokenListFields(Product product, ProductTokenInput addOnTokenInput,
                                         PriceItemTokenInput priceItemTokenInput) {
        product.getAddOns().clear();
        product.getAddOns().addAll(addOnTokenInput.getTokenObjects());
        product.setPrimaryPriceItem(priceItemTokenInput.getItem());
    }

    /** Returns the genotyping chip family and name for the product, product order, and date. */
    public Pair<String, String> getGenotypingChip(ProductOrderSample productOrderSample, Date effectiveDate) {
        String productPartNumber = productOrderSample.getProductOrder().getProduct().getPartNumber();
        String productOrderName = productOrderSample.getProductOrder().getName();
        return getGenotypingChip(productPartNumber, productOrderName, effectiveDate);
    }

    /**
     * Returns the genotyping chip family and name for the product part number and product order
     * using the historical genotyping chip mappings that existed on the effective date.
     *
     * @return (chip technology, chip name) or (null, null) if no match was found.
     */
    public Pair<String, String> getGenotypingChip(String productPartNumber, String productOrderName,
                                                  Date effectiveDate) {

        // Retrieves the historical chip mappings and uses them to find the best match for the product part number
        // and pdo name. There are multiple chips for a product part number when substring matching on the product
        // order name is used. Matches substrings in increasing order of complexity (substring length) so that the
        // last match is the best one.
        List<AttributeArchetype> archetypes = auditReaderDao.getVersionsAsOf(AttributeArchetype.class, effectiveDate);

        AttributeArchetype bestMatch = null;
        SortedMap<String, AttributeArchetype> partialMatches = partNumberMatches(productPartNumber, archetypes);
        for (String key : partialMatches.keySet()) {
            // key consists of part number and possibly also a substring.
            String[] keySplit = key.split(DELIMITER, 2);
            if (keySplit.length == 1 || productOrderName.contains(keySplit[1])) {
                bestMatch = partialMatches.get(key);
            }
        }
        String chipFamily = (bestMatch != null) ?
                bestMatch.getAttributeMap().get(Product.GENOTYPING_CHIP_TECHNOLOGY) : null;
        String chipTypeName = (bestMatch != null) ?
                bestMatch.getAttributeMap().get(Product.GENOTYPING_CHIP_NAME) : null;
        return Pair.of(chipFamily, chipTypeName);
    }

    /**
     * Returns the product configuration genotyping chip mappings that match on the part number.
     * @param productPartNumber  part number to match
     * @param allArchetypes collection of archetypes and may include other namespaces and groups.
     * @return  map of (product part number + delimiter + pdoString) -> archetype
     */
    private SortedMap<String, AttributeArchetype> partNumberMatches(String productPartNumber,
                                                                    Collection<AttributeArchetype> allArchetypes) {
        SortedMap<String, AttributeArchetype> matches = new TreeMap<>();

        for (AttributeArchetype archetype : allArchetypes) {
            if (archetype.getNamespace().equals(namespace) && archetype.getGroup().equals(group) &&
                productPartNumber.equals(archetype.getArchetypeName().split(DELIMITER)[0])) {

                matches.put(archetype.getArchetypeName(), archetype);
            }
        }
        return matches;
    }

    // Updates, deletes, adds the mappings for a product part number to genotyping chips.
    // Corresponding elements of chipNames, pdoStrings, chipTechnology have the same array index.
    private void persistGenotypingChipMappings(String productPartNumber, String[] genotypingChipTechnologies,
                                               String[] genotypingChipNames, String[] genotypingChipPdoSubstrings) {

        SortedMap<String, AttributeArchetype> currentMappings = partNumberMatches(productPartNumber,
                attributeArchetypeDao.findByGroup(Product.class.getCanonicalName(), Product.GENOTYPING_CHIP_CONFIG));

        for (int i = 0; i < genotypingChipTechnologies.length; ++i) {
            String chipName = genotypingChipNames[i];
            String chipTechnology = genotypingChipTechnologies[i];

            String key = productPartNumber;
            if (genotypingChipPdoSubstrings != null && i < genotypingChipPdoSubstrings.length &&
                StringUtils.isNotBlank(genotypingChipPdoSubstrings[i])) {
                key += DELIMITER + genotypingChipPdoSubstrings[i];
            }

            AttributeArchetype archetype = currentMappings.remove(key);
            if (archetype == null) {
                // A new mapping consists of an archetype having two attributes.
                // The archetype name is the product part number + delimiter + pdo name substring.
                // Its attributes are the genotyping chip technology and the genotyping chip name.
                AttributeArchetype attributeArchetype = new AttributeArchetype(namespace, group, key);
                attributeArchetype.getAttributes().add(new ArchetypeAttribute(attributeArchetype,
                        Product.GENOTYPING_CHIP_TECHNOLOGY, chipTechnology));
                attributeArchetype.getAttributes().add(new ArchetypeAttribute(attributeArchetype,
                        Product.GENOTYPING_CHIP_NAME, chipName));
                attributeArchetypeDao.persist(attributeArchetype);
            } else {
                for (ArchetypeAttribute attribute : archetype.getAttributes()) {
                    if (attribute.getAttributeName().equals(Product.GENOTYPING_CHIP_TECHNOLOGY)) {
                        attribute.setAttributeValue(chipTechnology);
                    } else if (attribute.getAttributeName().equals(Product.GENOTYPING_CHIP_NAME)) {
                        attribute.setAttributeValue(chipName);
                    }
                }
            }
        }

        // Deletes the remaining mappings which were not found in the given arrays.
        for (AttributeArchetype archetype : currentMappings.values()) {
            attributeArchetypeDao.remove(archetype);
        }
    }

    /** Returns the chip technologies found among the existing chip mappings. */
    public Set<String> findChipTechnologies() {
        Set<String> chipTechnologies = new HashSet<>();
        for (Triple<String, String, String> triple : getMappedGenotypingChips(null)) {
            chipTechnologies.add(triple.getLeft());
        }
        return chipTechnologies;
    }

    /**
     * Returns info on the genotyping chips mapped to the given product. Multiple mappings are
     * distinguished from one another using pdoSubstring.
     *
     * @param productPartNumber is used to determine the returned info. If null, returns all mappings found.
     * @return List of chip technology, chip name, pdo substring.
     */
    public List<Triple<String, String, String>> getMappedGenotypingChips(String productPartNumber) {
        List<Triple<String, String, String>> chipInfo = new ArrayList<>();

        for (AttributeArchetype archetype : attributeArchetypeDao.findByGroup(namespace, group)) {
            String[] partNumberAndPdoString = archetype.getArchetypeName().split(DELIMITER);
            if (productPartNumber == null || productPartNumber.equals(partNumberAndPdoString[0])) {

                chipInfo.add(Triple.of(
                        archetype.getAttributeMap().get(Product.GENOTYPING_CHIP_TECHNOLOGY),
                        archetype.getAttributeMap().get(Product.GENOTYPING_CHIP_NAME),
                        (partNumberAndPdoString.length > 1) ? partNumberAndPdoString[1] : null));

            }
        }
        return chipInfo;
    }
}
