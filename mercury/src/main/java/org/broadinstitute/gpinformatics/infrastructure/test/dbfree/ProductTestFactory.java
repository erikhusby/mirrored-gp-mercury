package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

public class ProductTestFactory {
    public static Product createTestProduct() {
        String uuid = UUID.randomUUID().toString();
        return createDummyProduct(Workflow.AGILENT_EXOME_EXPRESS, "partNumber " + uuid);
    }

    public static Product createDummyProduct(Workflow workflow, String partNumber) {
        return createDummyProduct(workflow, partNumber, false);
    }

    public static Product createDummyProduct(Workflow workflow, String partNumber, boolean addRisk) {
        Product product =
                new Product("productName", new ProductFamily("Test product family"), "description", partNumber,
                        new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements", "deliverables", true,
                        workflow, false, "an aggregation data type");
        if (addRisk) {
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.MANUAL, Operator.IS, "true"));
        }
        return product;
    }

    public static Product createStandardExomeSequencing(){
        Product product=null;
        try {
            product = new Product("Standard Exome Sequencing v3", new ProductFamily("Exome"),
                    "The Standard Exome includes sample plating, library preparation, hybrid capture, sequencing (76bp paired reads), sample identification QC check, and data storage. This product utilizes Broad dual-barcoded library construction followed by the Illumina Rapid Capture Exome enrichment kit with 38Mb target territory (29Mb baited). Our hybrid selection libraries typically meet or exceed 80% of targets at 20x and a mean target coverage >80x. We have the ability to process up to 368 samples per week handled in batches of 92. Processing times vary and depend on current demand. For additional capacity please contact genomics@broadinstitute.org",
                    "P-EX-0006",
                    DateUtils.parseDate("06/01/2013"), new Date(), 12345678, 123456, 100, 96,
                    "Funding and compliance requirements must be in place - this includes a valid IRB or letter of non-engagement where needed Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender Genomic DNA, fresh frozen or FFPE tissue, blood, stool, saliva, slides, cell pellets, or buffy coats that preferably yield >250ng of DNA (note extra cost will be applied for extractions). Samples below 250ng (2ng/uL minimum concentration), WGA and FFPE samples will be accepted at risk, but success rates are high with 50ng (1ng/uL minimum concentration) or greater. Samples below 40ng are not accepted Tumor/Normal or Case/Control pairs must be received together if indel co-cleaning is required. Samples Lab Pico results from within the last year",
                    "Data delivery will include a de-multiplexed, aggregated Picard BAM file which will be accessed via the BASS file server system or FTP for non-Broad users. Data storage for 5 years is provided.",
                    true, Workflow.WHOLE_GENOME, false, null);
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "2"));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.FFPE, Operator.IS, null));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.WGA, Operator.IS, null));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.PICO_AGE, Operator.IS, "true"));
            product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.TOTAL_DNA, Operator.LESS_THAN, ".250"));
        } catch (ParseException e) {

        }
        return product;
    }


}
