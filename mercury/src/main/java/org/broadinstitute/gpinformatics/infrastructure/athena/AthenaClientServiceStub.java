package org.broadinstitute.gpinformatics.infrastructure.athena;

import clover.org.apache.commons.lang.StringUtils;
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
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;

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

    @Override
    public ProductOrder retrieveProductOrderDetails(String poBusinessKey) {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = buildTestProductOrderMap();

        ProductOrder testOrder1 = productOrderByBusinessKeyMap.get(poBusinessKey);
        if (testOrder1 == null) {
            testOrder1 = createDummyProductOrder();
            testOrder1.setJiraTicketKey(poBusinessKey);
        }
        productOrderByBusinessKeyMap.put(poBusinessKey, testOrder1);

        if (poBusinessKey == null) {
            testOrder1.getProduct().setWorkflowName(null);
        }

        return testOrder1;
    }

    @Override
    public Map<String, List<ProductOrderSample>> findMapBySamples(List<String> sampleNames) {
        Map<String, List<ProductOrderSample>> mapSampleIdToPdoSample = new HashMap<String, List<ProductOrderSample>>();
        ProductOrder productOrder = buildExExProductOrder();
        List<ProductOrderSample> samples = productOrder.getSamples();
        for (ProductOrderSample productOrderSample : samples) {
            mapSampleIdToPdoSample.put(productOrderSample.getSampleName(),
                    new ArrayList<ProductOrderSample>(Collections.singletonList(productOrderSample)));
        }
        return mapSampleIdToPdoSample;
    }

    private Map<String, ProductOrder> buildTestProductOrderMap() {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<String, ProductOrder>();
        ProductOrder tempPO = createDummyProductOrder();
        Random random = new Random();

        tempPO.setJiraTicketKey("PDO-" + (random.nextInt() * 11));

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

        String workflowName = WorkflowName.EXOME_EXPRESS.getWorkflowName();
        String pdoBusinessName = "PD0-1EE";

        return createDummyProductOrder(maxSamples, pdoBusinessName, workflowName, 101L, "Test RP", rpSynopsis,
                                              ResearchProject.IRB_ENGAGED, "P-EXEXTest-1232");

    }

    public static ProductOrder buildHybridSelectionProductOrder(int maxSamples) {

        String jiraKey = "PD0-1HS";

        return createDummyProductOrder(maxSamples, jiraKey,
                                              WorkflowName.HYBRID_SELECTION.getWorkflowName(), 101L,
                                              "Test RP", rpSynopsis,
                                              ResearchProject.IRB_ENGAGED, "P-HSEL-9293");
    }

    public static ProductOrder buildWholeGenomeProductOrder(int maxSamples) {

        String jiraKey = "PD0-2WGS";

        return createDummyProductOrder(maxSamples, jiraKey, WorkflowName.WHOLE_GENOME.getWorkflowName(),
                                              301L, "Test RP", rpSynopsis,
                                              ResearchProject.IRB_ENGAGED, "P-WGS-9294");

    }

    public static ProductOrder createDummyProductOrder(int sampleCount, String jiraKey, String workflowName,
                                                       long creatorId, String rpTitle, String rpSynopsis,
                                                       boolean irbNotEngaged, String productPartNumber) {

        PriceItem priceItem = new PriceItem(PriceItem.PLATFORM_GENOMICS, PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                                                   PriceItem.NAME_EXOME_EXPRESS, "testQuoteId");
        Product dummyProduct = createDummyProduct(workflowName, productPartNumber);
        dummyProduct.setPrimaryPriceItem(priceItem);

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>(sampleCount);
        // starting rack
        for (int sampleIndex = 1; sampleIndex <= sampleCount; sampleIndex++) {
            String bspStock = "SM-" + String.valueOf(sampleIndex) + String.valueOf(sampleIndex + 1) +
                                      String.valueOf(sampleIndex + 3) + String.valueOf(sampleIndex + 2);
            productOrderSamples.add(new ProductOrderSample(bspStock, BSPSampleDTO.DUMMY));
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

    public static ProductOrder createDummyProductOrder(int sampleCount, String jiraKey, String workflowName,
                                                       long creatorId) {

        return createDummyProductOrder(sampleCount, jiraKey, workflowName, creatorId, "MyResearchProject",
                                              "To Study Stuff",
                                              ResearchProject.IRB_ENGAGED, "partNumber");
    }

    /*
       helper Methods to create test data.  Moved from Test cases to aid stub implementation
    */
    public static ProductOrder createDummyProductOrder() {
        return createDummyProductOrder(1, null, WorkflowName.EXOME_EXPRESS.getWorkflowName(), 10950L);
    }

    public static Product createDummyProduct(String workflowName, String partNumber) {
        return new Product("productName", new ProductFamily("Test product family"), "description", partNumber,
                                  new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements",
                                  "deliverables", true, workflowName, false);
    }

    public static ResearchProject createDummyResearchProject(Long createdBy, String title, String synopsis,
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

        researchProject.addPerson(RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);
        researchProject.addPerson(RoleType.BROAD_PI, 10950L);
        researchProject.addPerson(RoleType.BROAD_PI, 10951L);
        return researchProject;
    }

}
