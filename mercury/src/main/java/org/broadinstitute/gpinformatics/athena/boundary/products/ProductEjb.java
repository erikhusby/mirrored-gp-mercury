package org.broadinstitute.gpinformatics.athena.boundary.products;


import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.presentation.run.GenotypingChipTypeActionBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;

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
     *  @param product The product with all updated date (it was found for edit).
     * @param addOnTokenInput The add ons
     * @param priceItemTokenInput The price items
     * @param allLengthsMatch Do the lengths match
     * @param criteria The risk criteria
     * @param operators The operators
     * @param values The values
     */
    public void saveProduct(
            Product product, ProductTokenInput addOnTokenInput, PriceItemTokenInput priceItemTokenInput,
            boolean allLengthsMatch, String[] criteria,
            String[] operators, String[] values) {

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

    /** Returns the genotyping chip family and name for the product, product order, and date. */
    public Pair<String, String> getGenotypingChip(String productPartNumber, String productOrderName, Date effectiveDate) {
        // Finds the genotyping chip that maps to the product and optionally to the pdo.
        String chipTypeName = null;
        String chipFamily = null;
        Collection<AttributeArchetype> archetypes = attributeArchetypeDao.findByGroup(
                Product.class.getCanonicalName(), Product.GENOTYPING_CHIP_CONFIG);
        for (AttributeArchetype productConfigArchetype : archetypes) {
            // The archetype name consists of the product part number and an optional string.
            // When present it must match a substring in the product order name.
            String[] ppnAndPdoName = productConfigArchetype.getArchetypeName().split(" ", 2);
            if (productPartNumber.equals(ppnAndPdoName[0]) &&
                (ppnAndPdoName.length == 1 || productOrderName.contains(ppnAndPdoName[1]))) {
                // Gets the chip name and chip technology from the attributes.
                for (ArchetypeAttribute attribute : productConfigArchetype.getAttributes()) {
                    if (attribute.getAttributeName().equals(Product.GENOTYPING_CHIP_TECHNOLOGY)) {
                        chipFamily = attribute.getAttributeValue();
                    }
                    if (attribute.getAttributeName().equals(Product.GENOTYPING_CHIP_NAME)) {
                        // Users may occasionally change the type of chip used for a product.
                        // Retrieves the chip name from the mapping on the effective date.
                        ArchetypeAttribute versionedAttribute = auditReaderDao.getVersionAsOf(
                                ArchetypeAttribute.class, attribute.getAttributeId(), effectiveDate, true);
                        chipTypeName = versionedAttribute.getAttributeValue();
                    }
                }
            }
        }
        return Pair.of(chipFamily, chipTypeName);
    }
}
