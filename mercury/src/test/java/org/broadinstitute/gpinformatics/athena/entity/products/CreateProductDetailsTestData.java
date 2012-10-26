package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Calendar;

import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Category.*;
import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Name.*;
import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.Platform.GP;


/**
 * "Test" to create some basic {@link ProductFamily}, {@link PriceItem}, and {@link Product} test fixture data.
 * This will probably need to be turned into a Liquibase equivalent as well.
 *
 */
public class CreateProductDetailsTestData extends ContainerTest {

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;


    private void createProductFamilies() {

        for (ProductFamily.ProductFamilyName productFamilyName : ProductFamily.ProductFamilyName.values()) {

            if ( productFamilyDao.find(productFamilyName) == null ) {

                ProductFamily pf = new ProductFamily(productFamilyName.name());
                productFamilyDao.persist(pf);
            }
        }
    }


    private ProductFamily findProductFamily(ProductFamily.ProductFamilyName productFamilyName) {
        return productFamilyDao.find(productFamilyName);
    }


    @Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
    public void createTestData() {

        createProductFamilies();

        ProductFamily exomeSequencingAnalysisProductFamily =
                findProductFamily(ProductFamily.ProductFamilyName.EXOME_SEQUENCING_ANALYSIS);

        ProductFamily generalProductsProductFamily =
                findProductFamily(ProductFamily.ProductFamilyName.GENERAL_PRODUCTS);

        ProductFamily illuminaSequencingOnlyProductFamily =
                findProductFamily(ProductFamily.ProductFamilyName.ILLUMINA_SEQUENCING_ONLY);

        PriceItem labTimePriceItem = new PriceItem(
                GP,
                GENERAL_PRODUCTS,
                TIME_AND_MATERIALS_LAB,
                "GP-TIME_AND_MATERIALS_LAB-2012.11.01"
        );


        PriceItem ifxTimePriceItem = new PriceItem(
                GP,
                GENERAL_PRODUCTS,
                TIME_AND_MATERIALS_IFX,
                "GP-TIME_AND_MATERIALS_IFX-2012.11.01"
        );



        final int DAYS = 24 * 60 * 60;

        Product exex = new Product(

                "Exome Express",                       // product name

                exomeSequencingAnalysisProductFamily,  // product family

                                                       // description
                "The Exome Express combines the quality and best practices of the Genomics Platform Exome pipeline " +
                "with a uniquely designed workflow optimized for speed.  Our in-solution hybrid selection libraries " +
                "utilizing the Agilent Sure-Select Human All Exon v2.0, 44Mb baited target region meet or exceed " +
                "80% of targets at 20x and a mean target coverage >80x.",

                "EXOME_EXPRESS-2012.11.01",            // part number

                Calendar.getInstance().getTime(),      // availability date

                null,                                  // discontinued date

                17 * DAYS,                             // expected cycle time

                21 * DAYS,                             // guaranteed cycle time

                188,                                   // samples per week
                96,                                   // min order size

                                                       // input requirements
                "Funding and compliance requirements must be in place - this includes a valid IRB or letter of " +
                "non-engagement where needed\n" +
                "Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender\n" +
                "Genomic DNA, Tissue, Blood, Slides and FFPE that preferably yield >500ng of DNA (note extra cost " +
                "will be applied for extractions). Samples below 200ng and FFPE will be accepted at risk.\n" +
                "Tumor/Normal  or Case/Control pairs must be received together\n" +
                "Samples will be proceed on risk unless DNA QC yield is zero\n",

                                                       // deliverables
                "Data delivery will include a de-multiplexed, aggregated BAM file which will be accessed via " +
                "the BASS file server system.  Turnaround time from verified* sample receipt to aggregated BAM file " +
                "generation is 21 calendar days or less. Samples that fail to meet this deliverable will be charged " +
                "at the lower standard exome rate",

                true,                                  // top level product

                "EXEX-WF-2012.11.01"                   // workflow name
        );


        exex.addPriceItem(labTimePriceItem);
        exex.addPriceItem(ifxTimePriceItem);


        PriceItem priceItem;

        priceItem = new PriceItem(
                GP,                                   // platform
                EXOME_SEQUENCING_ANALYSIS,            // category name
                EXOME_EXPRESS,                        // price item name
                "GP-EXOME_ANALYSIS-EXEX-2012.11.01"   // quote server price item id
        );

        exex.addPriceItem(priceItem);
        exex.setDefaultPriceItem(priceItem);

        priceItem = new PriceItem(
                GP,                                    // platform
                EXOME_SEQUENCING_ANALYSIS,             // category name
                STANDARD_EXOME_SEQUENCING,             // price item name
                "GP-EXOME_ANALYSIS-STDEX-2012.11.01"   // quote server price item id
        );

        exex.addPriceItem(priceItem);

        Product dnaExtraction = new Product(

                "DNA Extraction",                       // product name

                generalProductsProductFamily,           // product family

                                                        // description
                "The Genomics Platform performs high quality DNA extractions from a variety of sample types:  blood " +
                "(fresh or frozen), cells, and tissue (fresh, snap-frozen, Formalin-Fixed-Paraffin-Embedded, PAXgene " +
                "Preserved), etc.  DNA is extracted using a column-based DNeasy Kit.  The samples are first lysed " +
                "with Proteinase K.  Buffering conditions are adjusted so to provide optimal DNA binding conditions " +
                "to the DNeasy spin column.  Once the lysed sample is added to the column, DNA is selectively bound " +
                "to the column membrane as contaminants and enzyme inhibitors pass through in the wash steps.  " +
                "The DNA is then eluted off the column with TE buffer and is ready to be quantified via picogreen.",

                "DNA_EXTRACTION-2012.11.01",           // part number

                Calendar.getInstance().getTime(),      // availability date

                null,                                  // discontinued date

                2 * DAYS,                              // expected cycle time

                3 * DAYS,                              // guaranteed cycle time

                360,                                   // samples per week

                192,                                   // min order size

                                                       // input requirements
                "Funding and compliance requirements must be in place - this includes a valid IRB or letter of " +
                "non-engagement where needed\n" +
                "Minimum Sample data including - Collaborator Participant ID, Collaborator Sample ID, Gender\n",

                                                       // deliverables
                "High quality DNA (A260/A280 range of 1.7-1.9).  DNA is quantified in triplicate using a standardized " +
                "picogreen assay.  Sample yields is dependent on multiple factors such as:   the original material " +
                "type provided (blood, cells, tissue, etc), amount of material provided, and tissue site.",

                true,                                  // top level product

                "DNA_EXTRACT-WF-2012.11.01"            // workflow name

        );

        dnaExtraction.addPriceItem(labTimePriceItem);
        dnaExtraction.addPriceItem(ifxTimePriceItem);

        exex.addAddOn(dnaExtraction);

        priceItem = new PriceItem(
                GP,                                                      // platform
                GENERAL_PRODUCTS,                                        // category name
                DNA_EXTRACTION,                                          // price item name
                "GP-GENERAL_PRODUCTS-DNA_EXTRACTION_2012.11.01"          // quote server price item id
        );
        dnaExtraction.addPriceItem(priceItem);
        dnaExtraction.setDefaultPriceItem(priceItem);

        Product extraCoverage = new Product(
             "Extra HiSeq Coverage",                    // product name
             illuminaSequencingOnlyProductFamily,       // product family
             "More seq data",                           // description
             "EXTRA_HISEQ_COVERAGE-2012.11.01",         // part number
             Calendar.getInstance().getTime(),          // availability date
             null,                                      // discontinued date
             3 * DAYS,                                  // expected cycle time
             4 * DAYS,                                  // guaranteed cycle time
             192,                                       // samples per week
             192,                                       // min order size
             "Same as before",                          // input requirements
             "Bigger BAM file",                         // deliverables
             false,                                     // top level product
             "GP-ILLUMINA_SEQUENCING_ONLY-EXTRA_HISEQ_COVERAGE_2012.11.01" // quote server price item id

        );

        extraCoverage.addPriceItem(labTimePriceItem);
        extraCoverage.addPriceItem(ifxTimePriceItem);

        exex.addAddOn(extraCoverage);

        priceItem = new PriceItem(
                GP,                                                              // platform
                ILLUMINA_SEQUENCING_ONLY,                                        // category name
                EXTRA_HISEQ_COVERAGE,                                            // price item name
                "GP-ILLUMINA_SEQUENCING_ONLY-EXTRA_HISEQ_COVERAGE_2012.11.01"    // quote server price item id
        );

        extraCoverage.addPriceItem(priceItem);
        extraCoverage.setDefaultPriceItem(priceItem);

        productDao.persist(exex);

    }

}
