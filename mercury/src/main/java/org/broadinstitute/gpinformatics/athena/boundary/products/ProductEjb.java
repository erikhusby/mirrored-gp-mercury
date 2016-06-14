package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.PriceItemTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

@Stateful
@RequestScoped
/**
 * Transactional manager for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s.
 */
public class ProductEjb {
    private final ProductDao productDao;

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
     * @param genotypingChipInfo Genotyping chips for this product
     */
    public void saveProduct(
            Product product, ProductTokenInput addOnTokenInput, PriceItemTokenInput priceItemTokenInput,
            boolean allLengthsMatch, String[] criteria, String[] operators, String[] values,
            List<Triple<String, String, String>> genotypingChipInfo) {

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
        persistGenotypingChipMappings(product.getPartNumber(), genotypingChipInfo);
    }

    private void populateTokenListFields(Product product, ProductTokenInput addOnTokenInput,
                                         PriceItemTokenInput priceItemTokenInput) {
        product.getAddOns().clear();
        product.getAddOns().addAll(addOnTokenInput.getTokenObjects());
        product.setPrimaryPriceItem(priceItemTokenInput.getItem());
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
     * @return (chip family, chip name) or (null, null) if no match was found.
     */
    public Pair<String, String> getGenotypingChip(ProductOrder productOrder, Date effectiveDate) {
        String productPartNumber = productOrder.getProduct().getPartNumber();
        String productOrderName = productOrder.getName();
        return getGenotypingChip(productPartNumber, productOrderName, effectiveDate);
    }

    /**
     * Looks up a genotyping chip mapping that was active on the effective date.
     * @return (chip family, chip name) or (null, null) if no match was found.
     */
    public Pair<String, String> getGenotypingChip(String productPartNumber, String productOrderName,
                                                  Date effectiveDate) {

        // Retrieves the historical chip mappings and uses them to find the best match for the product part number
        // and pdo name.
        Collection<GenotypingChipMapping> mappings = attributeArchetypeDao.getMappingsAsOf(effectiveDate);

        // Only one of each mapping may be active for this date, i.e. mappings should be unique on product
        // part number and pdo substring.
        Set<String> uniquePartNumberAndSubstring = new HashSet<>();
        for (GenotypingChipMapping mapping : mappings) {
            if (!uniquePartNumberAndSubstring.add(mapping.getArchetypeName())) {
                throw new RuntimeException("Multiple genotyping chip mappings for '" + mapping.getArchetypeName() +
                                           "' on " + DateUtils.convertDateTimeToString(effectiveDate));
            }
        }

        // Does substring matching on the product order name when there are multiple chips for a product part number.
        // The map keys are in search order, same as shown in UI for Product chip mappings.
        SortedMap<String, GenotypingChipMapping> partialMatches = partNumberMatches(productPartNumber, mappings);
        GenotypingChipMapping bestMatch = null;
        for (Map.Entry<String, GenotypingChipMapping> entry : partialMatches.entrySet()) {
            if (productOrderName.contains(entry.getValue().getPdoSubstring())) {
                bestMatch = partialMatches.get(entry.getKey());
                break;
            }
        }
        return (bestMatch != null) ?
                Pair.of(bestMatch.getChipFamily(), bestMatch.getChipName()) : Pair.of((String)null, (String)null);
    }

    /**
     * Returns the product configuration genotyping chip mappings that match on the part number,
     * ordered by decreasing pdoString, nulls last.
     *
     * @param productPartNumber  part number to match
     * @param allChips collection of archetypes and may include other namespaces and groups.
     * @return  map of (product part number + delimiter + pdoString) -> archetype
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

    // Updates, deletes, adds the mappings for a product part number to genotyping chips.
    private void persistGenotypingChipMappings(String productPartNumber,
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

            GenotypingChipMapping chip = currentMappings.remove(mappingName);
            if (chip == null) {
                attributeArchetypeDao.persist(new GenotypingChipMapping(mappingName, chipFamily, chipName, now));
            } else {
                chip.setChipTechnology(chipFamily);
                chip.setChipName(chipName);
            }
        }

        // Inactivates the remaining mappings which were not found in the given arrays.
        for (GenotypingChipMapping mapping : currentMappings.values()) {
            mapping.setInactiveDate(now);
        }
    }

    /**
     * Returns info on the genotyping chips mapped currently mapped to the given product. Multiple mappings are
     * distinguished from one another using pdoSubstring.
     *
     * @param productPartNumber is used to determine the returned info. Null returns no mappings.
     * @return List of chip family, chip name, pdo substring.  These are ordered the same way they
     *         are used to lookup a mapping: by decreasing pdo substring with nulls last.
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
}
