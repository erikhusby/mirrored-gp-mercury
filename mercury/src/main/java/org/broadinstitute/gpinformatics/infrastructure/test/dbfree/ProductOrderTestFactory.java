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
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
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
    public static final Long TEST_CREATOR = 1111L;
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
            productOrderSamples.add(new ProductOrderSample(bspStock, new BSPSampleDTO()));
        }

        ResearchProject researchProject = ResearchProjectTestFactory.createDummyResearchProject(
                creatorId, rpTitle, rpSynopsis, irbNotEngaged);

        ProductOrder productOrder =
                new ProductOrder(creatorId, "Test PO", productOrderSamples, "GSP-123", dummyProduct, researchProject);
        if (StringUtils.isNotBlank(jiraKey)) {
            productOrder.setJiraTicketKey(jiraKey);
        }
        if (StringUtils.isBlank(quoteId)){
            productOrder.setSkipQuoteReason(TEST_SKIP_QUOTE_REASON);
        }

        Product dummyAddOnProduct =
                ProductTestFactory.createDummyProduct(Workflow.NONE, "partNumber");
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
        return createDummyProductOrder(maxSamples, "PD0-1EE", Workflow.AGILENT_EXOME_EXPRESS, 101, "Test RP",
                rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-EXEXTest-1232", SAMPLE_SUFFIX, "ExExQuoteId");
    }

    public static ProductOrder buildIceProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-1IC", Workflow.ICE, 101, "Test RP",
                rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-ICEtest-1232", SAMPLE_SUFFIX, "ExExQuoteId");
    }

    public static ProductOrder buildHybridSelectionProductOrder(int maxSamples, String sampleSuffix) {
        return createDummyProductOrder(maxSamples, "PD0-1HS",
                Workflow.HYBRID_SELECTION, 101,
                "Test RP", rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-HSEL-9293", sampleSuffix, "ExExQuoteId");
    }

    public static ProductOrder buildWholeGenomeProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, JIRA_KEY, Workflow.WHOLE_GENOME,
                301, "Test RP", rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-WGS-9294", SAMPLE_SUFFIX, "ExExQuoteId");
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
                new Product("Product Name " + uuid, productFamily, "Product Description " + uuid, "P-" + uuid,
                        new Date(), null, 0, 0, 0, 1, "Input requirements", "Deliverables", true, Workflow.NONE,
                        false, "Aggregation Data Type");


        ResearchProject researchProject = new ResearchProject(-1L, "Research Project " + uuid, "Synopsis", false,
                                                              ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        researchProject.setJiraTicketKey("RP-" + uuid);

        List<ProductOrderSample> productOrderSamples = new ArrayList<>();
        for (String sampleName : sampleNames) {
            productOrderSamples.add(new ProductOrderSample(sampleName));
        }

        PriceItem priceItem = new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category", "PriceItem Name " + uuid);
        product.setPrimaryPriceItem(priceItem);

        ProductOrder productOrder =
                new ProductOrder(-1L, "PDO title " + uuid, productOrderSamples, "Quote-" + uuid, product,
                        researchProject);
        productOrder.setJiraTicketKey("PDO-" + uuid);
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
