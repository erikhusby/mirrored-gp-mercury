package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.PipelineDataType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

public class ProductTestFactory {
    private static final String DEFAULT_PART_NUMBER_PREFIX = "P-";

    public static Product createTestProduct() {
        return createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, generateProductPartNumber());
    }

    public static Product createDummyProduct(String workflow, String partNumber) {
        return createDummyProduct(workflow, partNumber, false, false);
    }

    public static Product createDummyProduct(String workflow, String partNumber, boolean addRisk,
                                             boolean pdmOrderableOnly) {
        Product product =
                new Product("productName", new ProductFamily("Test product family"), "description", partNumber,
                        new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements", "deliverables", true,
                        workflow, pdmOrderableOnly, new PipelineDataType(Aggregation.DATA_TYPE_EXOME, true));
        product.setReadLength(76);
        product.setPairedEndRead(true);
        if (addRisk) {
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.MANUAL, Operator.IS, "true"));
        }
        return product;
    }

    public static Product createStandardExomeSequencing() {
        return createStandardExomeSequencing(null, null);
    }


    public static Product createStandardExomeSequencing(String familyName, String productName) {
        Product product = null;
        productName = productName == null ? "Standard Exome Sequencing v3" : productName;
        familyName = familyName == null ? "Exome" : familyName;
        try {
            product = new Product(productName, new ProductFamily(familyName),
                    "The Standard Exome includes sample plating, library preparation, hybrid capture, sequencing (76bp paired reads), sample identification QC check, and data storage. This product utilizes Broad dual-barcoded library construction followed by the Illumina Rapid Capture Exome enrichment kit with 38Mb target territory (29Mb baited). Our hybrid selection libraries typically meet or exceed 80% of targets at 20x and a mean target coverage >80x. We have the ability to process up to 368 samples per week handled in batches of 92. Processing times vary and depend on current demand. For additional capacity please contact genomics@broadinstitute.org",
                    "P-EX-0006",
                    DateUtils.parseDate("06/01/2013"), new Date(), 12345678, 123456, 100, 96,
                    "*Funding and compliance requirements must be in place - this includes a valid IRB or letter of non-engagement where needed \n"
                    + "*Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender",
                    "Data delivery will include a de-multiplexed, aggregated Picard BAM file which will be accessed via the BASS file server system or FTP for non-Broad users. Data storage for 5 years is provided.",
                    true, Workflow.WHOLE_GENOME, false, new PipelineDataType(Aggregation.DATA_TYPE_EXOME, true));
            product.addRiskCriteria(
                    new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "2"));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.FFPE, Operator.IS, null));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.WGA, Operator.IS, null));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.PICO_AGE, Operator.IS, "true"));
            product.addRiskCriteria(
                    new RiskCriterion(RiskCriterion.RiskCriteriaType.TOTAL_DNA, Operator.LESS_THAN, ".250"));
            product.setLoadingConcentration(BigDecimal.valueOf(225));
            product.setPairedEndRead(true);
        } catch (ParseException e) {

        }
        return product;
    }

    public static String generateProductPartNumber() {
        int adjustedMaxLength = Product.MAX_PART_NUMBER_LENGTH - DEFAULT_PART_NUMBER_PREFIX.length();
        String filler = String.valueOf(Math.random() * 111).replace(".", "");
        String nanos = filler + System.nanoTime();
        if (nanos.length() > adjustedMaxLength) {
            nanos = StringUtils.substring(nanos, nanos.length() - adjustedMaxLength);
        }
        return DEFAULT_PART_NUMBER_PREFIX + nanos;
    }

}
