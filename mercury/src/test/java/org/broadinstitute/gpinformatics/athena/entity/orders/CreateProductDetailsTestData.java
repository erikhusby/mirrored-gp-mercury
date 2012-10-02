package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Calendar;

import static org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily.Name.EXOME_SEQUENCING_ANALYSIS;
import static org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily.Name.GENERAL_PRODUCTS;
import static org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily.Name.ILLUMINA_SEQUENCING_ONLY;

public class CreateProductDetailsTestData extends ContainerTest {

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;


    private void createProductFamilies() {

        for (Enum<ProductFamily.Name> e : ProductFamily.Name.values()) {
            ProductFamily pf = new ProductFamily();
            pf.setName(e.name());
            productFamilyDao.persist(pf);
        }
    }


    private ProductFamily findProductFamily(ProductFamily.Name productFamilyName) {
        return productFamilyDao.find(productFamilyName);
    }



    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void createTestData() {


        createProductFamilies();

        Product exex = new Product();

        exex.setPartNumber("EXOME_EXPRESS-2012-11-01");

        exex.setProductFamily(findProductFamily(EXOME_SEQUENCING_ANALYSIS));

        final int DAYS = 24 * 60 * 60;
        // say expected time is 17 days
        exex.setExpectedCycleTimeSeconds(17 * DAYS);

        // guaranteed time is 21 days
        exex.setGuaranteedCycleTimeSeconds(21 * DAYS);

        exex.setAvailabilityDate(Calendar.getInstance().getTime());
        exex.setDiscontinuedDate(null);

        exex.setDescription(
                "The Exome Express combines the quality and best practices of the Genomics Platform Exome pipeline " +
                "with a uniquely designed workflow optimized for speed.  Our in-solution hybrid selection libraries " +
                "utilizing the Agilent Sure-Select Human All Exon v2.0, 44Mb baited target region meet or exceed " +
                "80% of targets at 20x and a mean target coverage >80x."
        );

        exex.setInputRequirements(
                "Funding and compliance requirements must be in place - this includes a valid IRB or letter of  non-engagement where needed\n" +
                "Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender\n" +
                "Genomic DNA, Tissue, Blood, Slides and FFPE that preferably yield >500ng of DNA (note extra cost will be applied for extractions). Samples below 200ng and FFPE will be accepted at risk.\n" +
                "Tumor/Normal  or Case/Control pairs must be received together\n" +
                "Samples will be proceed on risk unless DNA QC yield is zero\n"
        );

        exex.setDeliverables(
                "Data delivery will include a de-multiplexed, aggregated BAM file which will be accessed via " +
                "the BASS file server system.  Turnaround time from verified* sample receipt to aggregated BAM file " +
                "generation is 21 calendar days or less. Samples that fail to meet this deliverable will be charged " +
                "at the lower standard exome rate");

        exex.setSamplesPerWeek(188);

        exex.setTopLevelProduct(true);


        Product tissueExtraction = new Product();

        exex.getAddOns().add(tissueExtraction);

        tissueExtraction.setPartNumber("TISSUE_DNA_EXTRACTION-2012-11-01");

        tissueExtraction.setProductFamily(findProductFamily(GENERAL_PRODUCTS));

        // total guesses on time
        tissueExtraction.setExpectedCycleTimeSeconds(2 * DAYS);
        tissueExtraction.setGuaranteedCycleTimeSeconds(3 * DAYS);

        tissueExtraction.setAvailabilityDate(Calendar.getInstance().getTime());
        tissueExtraction.setDiscontinuedDate(null);
        tissueExtraction.setDescription(
                "The Genomics Platform performs high quality DNA extractions from a variety of sample types:  blood " +
                "(fresh or frozen), cells, and tissue (fresh, snap-frozen, Formalin-Fixed-Paraffin-Embedded, PAXgene " +
                "Preserved), etc.  DNA is extracted using a column-based DNeasy Kit.  The samples are first lysed " +
                "with Proteinase K.  Buffering conditions are adjusted so to provide optimal DNA binding conditions " +
                "to the DNeasy spin column.  Once the lysed sample is added to the column, DNA is selectively bound " +
                "to the column membrane as contaminants and enzyme inhibitors pass through in the wash steps.  " +
                "The DNA is then eluted off the column with TE buffer and is ready to be quantified via picogreen.");


        tissueExtraction.setInputRequirements(
                "Funding and compliance requirements must be in place - this includes a valid IRB or letter of " +
                "non-engagement where needed\n" +
                "Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender\n"
        );


        tissueExtraction.setDeliverables(
                "High quality DNA (A260/A280 range of 1.7-1.9).  DNA is quantified in triplicate using a standardized " +
                "picogreen assay.  Sample yields is dependent on multiple factors such as:   the original material " +
                "type provided (blood, cells, tissue, etc), amount of material provided, and tissue site."

        );

        tissueExtraction.setSamplesPerWeek(360);
        tissueExtraction.setTopLevelProduct(true);


        Product bloodExtraction = new Product();
        exex.getAddOns().add(bloodExtraction);
        bloodExtraction.setPartNumber("WHOLE_BLOOD_DNA_EXTRACTION-2012-11-01");

        bloodExtraction.setProductFamily(findProductFamily(GENERAL_PRODUCTS));

        // total guesses on time
        bloodExtraction.setExpectedCycleTimeSeconds(1 * DAYS);
        bloodExtraction.setGuaranteedCycleTimeSeconds(2 * DAYS);

        bloodExtraction.setAvailabilityDate(Calendar.getInstance().getTime());
        bloodExtraction.setDiscontinuedDate(null);
        bloodExtraction.setDescription(
                "The Genomics Platform performs high quality DNA extractions from a variety of sample types:  blood " +
                "(fresh or frozen), cells, and tissue (fresh, snap-frozen, Formalin-Fixed-Paraffin-Embedded, PAXgene " +
                "Preserved), etc.  DNA is extracted using a column-based DNeasy Kit.  The samples are first lysed " +
                "with Proteinase K.  Buffering conditions are adjusted so to provide optimal DNA binding conditions " +
                "to the DNeasy spin column.  Once the lysed sample is added to the column, DNA is selectively bound " +
                "to the column membrane as contaminants and enzyme inhibitors pass through in the wash steps.  " +
                "The DNA is then eluted off the column with TE buffer and is ready to be quantified via picogreen.");


        bloodExtraction.setInputRequirements(
                "Funding and compliance requirements must be in place - this includes a valid IRB or letter of " +
                "non-engagement where needed\n" +
                "Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender\n"
        );


        bloodExtraction.setDeliverables(
                "High quality DNA (A260/A280 range of 1.7-1.9).  DNA is quantified in triplicate using a standardized " +
                "picogreen assay.  Sample yields is dependent on multiple factors such as:   the original material " +
                "type provided (blood, cells, tissue, etc), amount of material provided, and tissue site."

        );

        bloodExtraction.setSamplesPerWeek(360);
        bloodExtraction.setTopLevelProduct(true);


        Product extraCoverage = new Product();

        exex.getAddOns().add(extraCoverage);

        extraCoverage.setPartNumber("EXTRA_HISEQ_COVERAGE-2012-11-01");

        extraCoverage.setProductFamily(findProductFamily(ILLUMINA_SEQUENCING_ONLY));

        // total guesses on time
        extraCoverage.setExpectedCycleTimeSeconds(3 * DAYS);
        extraCoverage.setGuaranteedCycleTimeSeconds(4 * DAYS);
        extraCoverage.setAvailabilityDate(Calendar.getInstance().getTime());
        extraCoverage.setDiscontinuedDate(null);

        extraCoverage.setDescription(
                "More seq data"
        );


        extraCoverage.setInputRequirements(
                "Same as before"
        );


        extraCoverage.setDeliverables(
                "Bigger BAM file"
        );

        extraCoverage.setSamplesPerWeek(192);
        extraCoverage.setTopLevelProduct(false);


    }


}
