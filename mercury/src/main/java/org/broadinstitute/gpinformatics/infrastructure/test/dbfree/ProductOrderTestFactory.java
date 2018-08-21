package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder.OrderStatus.Submitted;

public class ProductOrderTestFactory {
    public static final String JIRA_KEY = "PD0-2WGS";
    public static final String SAMPLE_SUFFIX = "A";
    public static final String TEST_SKIP_QUOTE_REASON = "I am skipping the quote because I can.";
    public static final Long TEST_CREATOR = 18356L;
    public static final String pdoTitle= "Test synopsis";
    public static final String rpSynopsis = "Test synopsis";
    public static final String otherRpSynopsis = "To Study Stuff";

    public static ProductOrder createDummyProductOrder(int sampleCount, @Nonnull String jiraKey,
                                                       Workflow workflow, long creatorId, String rpTitle,
                                                       String rpSynopsis, boolean irbNotEngaged,
                                                       String productPartNumber, String sampleSuffix, String quoteId) {
        Product dummyProduct =
                ProductTestFactory.createDummyProduct(workflow, productPartNumber);

        if (StringUtils.isNotBlank(quoteId)){
        PriceItem exExPriceItem =
                new PriceItem(quoteId, PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                        PriceItem.NAME_EXOME_EXPRESS);
            dummyProduct.setPrimaryPriceItem(exExPriceItem);
        }

        List<ProductOrderSample> productOrderSamples = new ArrayList<>(sampleCount);
        for (int sampleIndex = 1; sampleIndex <= sampleCount; sampleIndex++) {
            String bspStock = "SM-" + String.valueOf(sampleIndex) + String.valueOf(sampleIndex + 1) +
                              String.valueOf(sampleIndex + 3) + String.valueOf(sampleIndex + 2) + sampleSuffix;
            productOrderSamples.add(new ProductOrderSample(bspStock, new BspSampleData()));
        }

        ResearchProject researchProject = ResearchProjectTestFactory.createDummyResearchProject(
                creatorId, rpTitle, rpSynopsis, irbNotEngaged);
        String testRpValue = "RP-99999";
        researchProject.setJiraTicketKey(testRpValue);
        ProductOrder productOrder =
                new ProductOrder(creatorId, "Test PO", productOrderSamples, "GSP-123", dummyProduct, researchProject);
        if (StringUtils.isNotBlank(jiraKey)) {
            productOrder.setJiraTicketKey(jiraKey);
        }
        if (StringUtils.isBlank(quoteId)){
            productOrder.setSkipQuoteReason(TEST_SKIP_QUOTE_REASON);
        }

        Product dummyAddOnProduct =
                ProductTestFactory.createDummyProduct(Workflow.NONE, productPartNumber+"-addon");
        dummyAddOnProduct.setProductName("addOnProduct");
        PriceItem exExAddOnPriceItem =
                new PriceItem(quoteId, PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                        PriceItem.NAME_STANDARD_WHOLE_EXOME);
        dummyAddOnProduct.setPrimaryPriceItem(exExAddOnPriceItem);


        productOrder.updateAddOnProducts(Collections.singletonList(dummyAddOnProduct));

        return productOrder;

    }

    /*
     *  Helper methods to create test data.  Moved from Test cases to aid stub implementation.
     */
    public static ProductOrder createDummyProductOrder() {
        return createDummyProductOrder("PDO-0");
    }

    public static ProductOrder createDummyProductOrder(@Nonnull String jiraTicketKey) {
        return createDummyProductOrder(1, jiraTicketKey, Workflow.AGILENT_EXOME_EXPRESS, 10950, "MyResearchProject",
                otherRpSynopsis, ResearchProject.IRB_ENGAGED, "partNumber", SAMPLE_SUFFIX, "ExExQuoteId"
        );
    }

    public static ProductOrder createDummyProductOrder(int numSamples, @Nonnull String jiraTicketKey) {
        return createDummyProductOrder(numSamples, jiraTicketKey, Workflow.AGILENT_EXOME_EXPRESS, 10950,
                "MyResearchProject", otherRpSynopsis, ResearchProject.IRB_ENGAGED, "partNumber", SAMPLE_SUFFIX,
                "ExExQuoteId");
    }



    public static Map<String, ProductOrder> buildTestProductOrderMap() {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<>();
        ProductOrder tempPO = createDummyProductOrder("PDO-" + (new Random().nextInt() * 11));

        productOrderByBusinessKeyMap.put(tempPO.getBusinessKey(), tempPO);

        ProductOrder tempPO2 = buildExExProductOrder(96);
        productOrderByBusinessKeyMap.put(tempPO2.getBusinessKey(), tempPO2);

        ProductOrder tempPO3 = buildHybridSelectionProductOrder(96, "A");
        productOrderByBusinessKeyMap.put(tempPO3.getBusinessKey(), tempPO3);

        ProductOrder tempPO4 = buildWholeGenomeProductOrder(96);
        productOrderByBusinessKeyMap.put(tempPO4.getBusinessKey(), tempPO4);

        return productOrderByBusinessKeyMap;
    }

    public static ProductOrder buildExExProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.AGILENT_EXOME_EXPRESS);
    }

    public static ProductOrder buildProductOrder(int maxSamples, String sampleSuffix, Workflow workflow) {
        return createDummyProductOrder(maxSamples, "PD0-1EE", workflow, 101, "Test RP",
                rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-" + workflow.name() + "-1232", sampleSuffix, "ExExQuoteId");
    }

    public static ProductOrder buildIceProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.ICE);
    }

    public static ProductOrder buildHybridSelectionProductOrder(int maxSamples, String sampleSuffix) {
        return buildProductOrder(maxSamples, sampleSuffix, Workflow.HYBRID_SELECTION);
    }

    public static ProductOrder buildWholeGenomeProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.WHOLE_GENOME);
    }

    public static ProductOrder buildPcrFreeProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.PCR_FREE);
    }

    public static ProductOrder buildPcrPlusProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.PCR_PLUS);
    }

    public static ProductOrder buildPcrPlusHyperPrepProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.PCR_PLUS_HYPER_PREP);
    }

    public static ProductOrder buildPcrFreeHyperPrepProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.PCR_FREE_HYPER_PREP);
    }

    public static ProductOrder builCustomSelectionProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.CUSTOM_SELECTION);
    }

    public static ProductOrder buildCellFreeHyperPrepProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.CELL_FREE_HYPER_PREP);
    }

    public static ProductOrder buildICEHyperPrepProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.ICE_EXOME_EXPRESS_HYPER_PREP);
    }

    public static ProductOrder buildSampleInitiationProductOrder(int maxSamples) {

        ProductOrder sampleInitiationProductOrder = createDummyProductOrder(maxSamples, JIRA_KEY, Workflow.NONE,
                TEST_CREATOR, pdoTitle,
                rpSynopsis,
                ResearchProject.IRB_ENGAGED, Product.SAMPLE_INITIATION_PART_NUMBER, SAMPLE_SUFFIX, null);
        sampleInitiationProductOrder.setProductOrderKit(ProdOrderKitTestFactory.createDummyProductOrderKit(maxSamples));
        sampleInitiationProductOrder.getProduct().setPartNumber(Product.SAMPLE_INITIATION_PART_NUMBER);
        ProductFamily sampleInitiationProductFamily = new ProductFamily(ProductFamily.SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE_NAME);
        sampleInitiationProductOrder.getProduct().setProductFamily(sampleInitiationProductFamily);
        sampleInitiationProductOrder.setSkipQuoteReason(ProductOrderTestFactory.TEST_SKIP_QUOTE_REASON);
        return sampleInitiationProductOrder;
    }

    public static ProductOrder buildFPProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PDO-1FP", Workflow.NONE, 101,
                "Test RP", rpSynopsis, ResearchProject.IRB_ENGAGED, "P-FPtest-1232", SAMPLE_SUFFIX, "ExExQuoteId");
    }

    public static ProductOrder buildSingleCellProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PDO-1SC", Workflow.NONE, 101,
                "Test Single Cell", rpSynopsis, ResearchProject.IRB_ENGAGED, "P-SCtest-1232", SAMPLE_SUFFIX, "ExExQuoteId");
    }

    public static ProductOrder buildArrayPlatingProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PDO-1ARR", Workflow.NONE, 101,
                "Test RP", rpSynopsis, ResearchProject.IRB_ENGAGED, "P-ARRtest-1232", SAMPLE_SUFFIX, "ExExQuoteId");
    }

    public static ProductOrder buildTruSeqStrandSpecificProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PDO-1TRUSS", Workflow.NONE, 101,
                "Test RP", rpSynopsis, ResearchProject.IRB_ENGAGED, "P-TRUSStest-1232", SAMPLE_SUFFIX, "ExExQuoteId");
    }

    public static ProductOrder buildInfiniumMethylationProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.INFINIUM_METHYLATION);
    }

    public static ProductOrder buildInfiniumProductOrder(int maxSamples) {
        return buildProductOrder(maxSamples, SAMPLE_SUFFIX, Workflow.INFINIUM);
    }


    /**
     * Database free creation of Product Order from scratch, including creation of a new Research Project and Product.
     *
     * @param sampleNames Names of samples for this PDO.
     * @return Transient PDO.
     */
    public static ProductOrder createProductOrder(String... sampleNames) {
        UUID uuid = UUID.randomUUID();
        ProductFamily productFamily = new ProductFamily("Product Family " + uuid);
        Product product =
                new Product("Product Name " + uuid, productFamily, "Product Description " + uuid, "P-X" + uuid,
                        new Date(), null, 0, 0, 0, 1, "Input requirements", "Deliverables", true, Workflow.NONE,
                        false, "Aggregation Data Type");


        ResearchProject researchProject = new ResearchProject(-1L, "Research Project " + uuid, "Synopsis", false,
                                                              ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        // X after dash, to prevent web page autocomplete randomly matching uuid
        researchProject.setJiraTicketKey("RP-X" + uuid);

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (String sampleName : sampleNames) {
            MercurySample mercurySample = new MercurySample(sampleName, MercurySample.MetadataSource.BSP);
            ProductOrderSample productOrderSample = new ProductOrderSample(sampleName);
            mercurySample.addProductOrderSample(productOrderSample);
            productOrderSamples.add(productOrderSample);
        }

        PriceItem priceItem = new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category", "PriceItem Name " + uuid);
        product.setPrimaryPriceItem(priceItem);

        ProductOrder productOrder =
                new ProductOrder(-1L, "PDO title " + uuid, productOrderSamples, "Quote-" + uuid, product,
                        researchProject);
        productOrder.setJiraTicketKey("PDO-X" + uuid);
        productOrder.setModifiedBy(-1L);
        productOrder.setOrderStatus(Submitted);
        return productOrder;
    }

    /**
     * Group the {@code ProductOrderSample}s by their Sample IDs into a {@code Multimap} ({@code Multimap}s can have
     * multiple values for the same key).
     * @param productOrder Input Product Order
     */
    public static Multimap<String, ProductOrderSample> groupBySampleId(ProductOrder productOrder) {
        return Multimaps.index(productOrder.getSamples(), new Function<ProductOrderSample, String>() {
            @Override
            public String apply(ProductOrderSample input) {
                return input.getName();
            }
        });
    }
}
