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
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

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

    public static ProductOrder createDummyProductOrder(int sampleCount, @Nonnull String jiraKey,
            WorkflowName workflowName, long creatorId, String rpTitle, String rpSynopsis, boolean irbNotEngaged,
            String productPartNumber, String sampleSuffix) {

        PriceItem exExPriceItem =
                new PriceItem("ExExQuoteId", PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                        PriceItem.NAME_EXOME_EXPRESS);
        Product dummyProduct =
                ProductTestFactory.createDummyProduct(workflowName.getWorkflowName(), productPartNumber);
        dummyProduct.setPrimaryPriceItem(exExPriceItem);

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
        productOrder.setOrderStatus(Submitted);

        Product dummyAddOnProduct =
                ProductTestFactory.createDummyProduct("DNA Extract from FFPE or Slides", "partNumber");
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
        return createDummyProductOrder(1, jiraTicketKey, WorkflowName.EXOME_EXPRESS, 10950, "MyResearchProject",
                AthenaClientServiceStub.otherRpSynopsis, ResearchProject.IRB_ENGAGED, "partNumber", "A");
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
        return createDummyProductOrder(maxSamples, "PD0-1EE", WorkflowName.EXOME_EXPRESS, 101, "Test RP",
                AthenaClientServiceStub.rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-EXEXTest-1232", "A");
    }

    public static ProductOrder buildHybridSelectionProductOrder(int maxSamples, String sampleSuffix) {
        return createDummyProductOrder(maxSamples, "PD0-1HS",
                WorkflowName.HYBRID_SELECTION, 101,
                "Test RP", AthenaClientServiceStub.rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-HSEL-9293", sampleSuffix);
    }

    public static ProductOrder buildWholeGenomeProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-2WGS", WorkflowName.WHOLE_GENOME,
                301, "Test RP", AthenaClientServiceStub.rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-WGS-9294", "A");
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
                        new Date(), null, 0, 0, 0, 1, "Input requirements", "Deliverables", true, "Workflow name",
                        false, "Aggregation Data Type");


        ResearchProject researchProject = new ResearchProject(-1L, "Research Project " + uuid, "Synopsis", false);
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
                return input.getSampleName();
            }
        });
    }
}
