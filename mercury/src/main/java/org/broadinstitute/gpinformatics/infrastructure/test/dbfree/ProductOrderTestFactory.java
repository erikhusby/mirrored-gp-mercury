package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProductOrderTestFactory {

    public static ProductOrder createDummyProductOrder(int sampleCount, @Nonnull String jiraKey, WorkflowName workflowName,
                                                       long creatorId, String rpTitle, String rpSynopsis,
                                                       boolean irbNotEngaged, String productPartNumber) {

        PriceItem priceItem = new PriceItem(PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                                                   PriceItem.NAME_EXOME_EXPRESS, "testQuoteId");
        Product dummyProduct =
                ProductTestFactory.createDummyProduct(workflowName.getWorkflowName(), productPartNumber);
        dummyProduct.setPrimaryPriceItem(priceItem);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>(sampleCount);
        for (int sampleIndex = 1; sampleIndex <= sampleCount; sampleIndex++) {
            String bspStock = "SM-" + String.valueOf(sampleIndex) + String.valueOf(sampleIndex + 1) +
                                      String.valueOf(sampleIndex + 3) + String.valueOf(sampleIndex + 2);
            productOrderSamples.add(new ProductOrderSample(bspStock, new BSPSampleDTO()));
        }

        ProductOrder productOrder = new ProductOrder(creatorId, "Test PO", productOrderSamples, "GSP-123", dummyProduct,
                                                            ResearchProjectTestFactory
                                                                    .createDummyResearchProject(creatorId, rpTitle,
                                                                            rpSynopsis,
                                                                            irbNotEngaged));
        if (StringUtils.isNotBlank(jiraKey)) {
            productOrder.setJiraTicketKey(jiraKey);
        }
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        productOrder
                .updateAddOnProducts(Collections
                        .singletonList(ProductTestFactory.createDummyProduct("DNA Extract from FFPE or Slides",
                                "partNumber")));

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
                AthenaClientServiceStub.otherRpSynopsis, ResearchProject.IRB_ENGAGED, "partNumber");
    }

    public static Map<String, ProductOrder> buildTestProductOrderMap() {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<String, ProductOrder>();
        ProductOrder tempPO = createDummyProductOrder("PDO-" + (new Random().nextInt() * 11));

        productOrderByBusinessKeyMap.put(tempPO.getBusinessKey(), tempPO);

        ProductOrder tempPO2 = buildExExProductOrder(96);
        productOrderByBusinessKeyMap.put(tempPO2.getBusinessKey(), tempPO2);

        ProductOrder tempPO3 = buildHybridSelectionProductOrder(96);
        productOrderByBusinessKeyMap.put(tempPO3.getBusinessKey(), tempPO3);

        ProductOrder tempPO4 = buildWholeGenomeProductOrder(96);
        productOrderByBusinessKeyMap.put(tempPO4.getBusinessKey(), tempPO4);

        return productOrderByBusinessKeyMap;
    }

    public static ProductOrder buildExExProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-1EE", WorkflowName.EXOME_EXPRESS, 101, "Test RP", AthenaClientServiceStub.rpSynopsis,
                        ResearchProject.IRB_ENGAGED, "P-EXEXTest-1232");
    }

    public static ProductOrder buildHybridSelectionProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-1HS",
                WorkflowName.HYBRID_SELECTION, 101,
                "Test RP", AthenaClientServiceStub.rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-HSEL-9293");
    }

    public static ProductOrder buildWholeGenomeProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-2WGS", WorkflowName.WHOLE_GENOME,
                301, "Test RP", AthenaClientServiceStub.rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-WGS-9294");
    }

}
