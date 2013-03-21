package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Alternative;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:47 PM
 */
@Stub
@Alternative
public class AthenaClientServiceStub implements AthenaClientService {

    private static final Long   TEST_CREATOR = 1111L;
    public static final  String rpSynopsis   = "Test synopsis";
    public static final String otherRpSynopsis = "To Study Stuff";

    @Override
    public ProductOrder retrieveProductOrderDetails(String poBusinessKey) {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = buildTestProductOrderMap();

        ProductOrder testOrder1 = productOrderByBusinessKeyMap.get(poBusinessKey);
        if (testOrder1 == null) {
            testOrder1 = createDummyProductOrder(poBusinessKey);
        }
        productOrderByBusinessKeyMap.put(poBusinessKey, testOrder1);

        if (poBusinessKey == null) {
            testOrder1.getProduct().setWorkflowName(null);
        }

        return testOrder1;
    }

    @Override
    public Map<String, List<ProductOrderSample>> findMapSampleNameToPoSample(List<String> sampleNames) {
        Map<String, List<ProductOrderSample>> mapSampleIdToPdoSample = new HashMap<String, List<ProductOrderSample>>();
        ProductOrder productOrder = buildExExProductOrder(96);
        List<ProductOrderSample> samples = productOrder.getSamples();
        for (ProductOrderSample productOrderSample : samples) {
            mapSampleIdToPdoSample.put(productOrderSample.getSampleName(),
                    new ArrayList<ProductOrderSample>(Collections.singletonList(productOrderSample)));
        }
        return mapSampleIdToPdoSample;
    }

    private static Map<String, ProductOrder> buildTestProductOrderMap() {

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
        return createDummyProductOrder(maxSamples, "PD0-1EE", WorkflowName.EXOME_EXPRESS, 101, "Test RP", rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-EXEXTest-1232");
    }

    public static ProductOrder buildHybridSelectionProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-1HS",
                WorkflowName.HYBRID_SELECTION, 101,
                "Test RP", rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-HSEL-9293");
    }

    public static ProductOrder buildWholeGenomeProductOrder(int maxSamples) {
        return createDummyProductOrder(maxSamples, "PD0-2WGS", WorkflowName.WHOLE_GENOME,
                301, "Test RP", rpSynopsis,
                ResearchProject.IRB_ENGAGED, "P-WGS-9294");
    }

    public static ProductOrder createDummyProductOrder(int sampleCount, @Nonnull String jiraKey, WorkflowName workflowName,
                                                       long creatorId, String rpTitle, String rpSynopsis,
                                                       boolean irbNotEngaged, String productPartNumber) {

        PriceItem priceItem = new PriceItem(PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                                                   PriceItem.NAME_EXOME_EXPRESS, "testQuoteId");
        Product dummyProduct = createDummyProduct(workflowName.getWorkflowName(), productPartNumber);
        dummyProduct.setPrimaryPriceItem(priceItem);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>(sampleCount);
        for (int sampleIndex = 1; sampleIndex <= sampleCount; sampleIndex++) {
            String bspStock = "SM-" + String.valueOf(sampleIndex) + String.valueOf(sampleIndex + 1) +
                                      String.valueOf(sampleIndex + 3) + String.valueOf(sampleIndex + 2);
            productOrderSamples.add(
                new ProductOrderSample(bspStock, new BSPSampleDTO(new HashMap<BSPSampleSearchColumn, String>())));
        }

        ProductOrder productOrder = new ProductOrder(creatorId, "Test PO", productOrderSamples, "GSP-123", dummyProduct,
                                                            createDummyResearchProject(creatorId, rpTitle, rpSynopsis,
                                                                                              irbNotEngaged));
        if (StringUtils.isNotBlank(jiraKey)) {
            productOrder.setJiraTicketKey(jiraKey);
        }
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        productOrder
                .updateAddOnProducts(Collections.singletonList(createDummyProduct("DNA Extract from FFPE or Slides",
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
                otherRpSynopsis, ResearchProject.IRB_ENGAGED, "partNumber");
    }

    public static Product createDummyProduct(String workflowName, String partNumber) {
        return new Product("productName", new ProductFamily("Test product family"), "description", partNumber,
                                  new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements",
                                  "deliverables", true, workflowName, false);
    }

    public static ResearchProject createDummyResearchProject(long createdBy, String title, String synopsis,
                                                             boolean irbNotEngaged) {
        ResearchProject researchProject = new ResearchProject(createdBy, title, synopsis, irbNotEngaged);

        Set<Funding> fundingList =
                Collections.singleton(new Funding(Funding.PURCHASE_ORDER, "A piece of Funding", "POFunding"));
        researchProject.populateFunding(fundingList);
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        Collection<Irb> irbs = Collections.singleton(new Irb("irbInitial", ResearchProjectIRB.IrbType.FARBER));
        researchProject.populateIrbs(irbs);
        researchProject
                .addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.BROAD, "irb123"));
        researchProject
                .addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.OTHER, "irb456"));

        researchProject.addPerson(RoleType.SCIENTIST, 111);
        researchProject.addPerson(RoleType.SCIENTIST, 222);
        researchProject.addPerson(RoleType.BROAD_PI, 10950);
        researchProject.addPerson(RoleType.BROAD_PI, 10951);
        return researchProject;
    }
}
