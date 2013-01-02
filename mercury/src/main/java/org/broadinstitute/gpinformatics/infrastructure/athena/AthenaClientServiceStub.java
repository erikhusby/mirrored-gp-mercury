package org.broadinstitute.gpinformatics.infrastructure.athena;

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
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;

import java.util.*;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:47 PM
 */
@Stub
public class AthenaClientServiceStub implements AthenaClientService {

    private static final Long TEST_CREATOR = 1111L;
    public static final  String  rpSynopsis = "Test synopsis";

    @Override
    public ProductOrder retrieveProductOrderDetails ( String poBusinessKey ) {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = buildTestProductOrderMap();



        ProductOrder testOrder1 = productOrderByBusinessKeyMap.get(poBusinessKey);
        if(testOrder1 == null) {
            testOrder1 = createDummyProductOrder();
        testOrder1.setJiraTicketKey(poBusinessKey);
        }
        productOrderByBusinessKeyMap.put(poBusinessKey,testOrder1);

//        if(!productOrderByBusinessKeyMap.containsKey(poBusinessKey)) {
//            throw new IllegalStateException("The key " + poBusinessKey + " does not map to a known ProductOrder");
//        }

        return testOrder1;
    }

    private Map<String, ProductOrder> buildTestProductOrderMap() {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<String, ProductOrder>();
        ProductOrder tempPO = createDummyProductOrder();
        Random random = new Random();

        tempPO.setJiraTicketKey("PDO-"+(random.nextInt()*11));

        productOrderByBusinessKeyMap.put(tempPO.getBusinessKey(), tempPO);
        ProductOrder tempPO2 = buildExExProductOrder();
        productOrderByBusinessKeyMap.put(tempPO2.getBusinessKey(), tempPO2);
        return productOrderByBusinessKeyMap;
    }

    private ProductOrder buildExExProductOrder() {

        String workflowName = "Exome Express";
        LinkedHashMap<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();

        Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();

        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>();

        ProductOrder productOrder = new ProductOrder(101L, "Test PO", productOrderSamples, "GSP-123",
                                                     new Product("Test product",
                                                                 new ProductFamily("Test product family"), "test",
                                                                 "1234", null, null, 10000, 20000, 100, 40, null, null,
                                                                 true, workflowName, false),
                                                     new ResearchProject(101L, "Test RP", rpSynopsis, false));
        String pdoBusinessName = "PDO-999";
        productOrder.setJiraTicketKey(pdoBusinessName);
        mapKeyToProductOrder.put(pdoBusinessName, productOrder);

        List<String> vesselSampleList = new ArrayList<String>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        // starting rack
        for (int sampleIndex = 1; sampleIndex <= vesselSampleList.size(); sampleIndex++) {
            String barcode = "R" + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex + sampleIndex;
            String bspStock = vesselSampleList.get(sampleIndex - 1);
            productOrderSamples.add(new ProductOrderSample(bspStock));
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(pdoBusinessName, bspStock));
            mapBarcodeToTube.put(barcode, bspAliquot);
        }

        return productOrder;
    }


    /*
        helper Methods to create test data.  Moved from Test cases to aid stub implementation
     */
    public static ProductOrder createDummyProductOrder() {
        PriceItem priceItem = new PriceItem(
                PriceItem.PLATFORM_GENOMICS,
                PriceItem.CATEGORY_EXOME_SEQUENCING_ANALYSIS,
                PriceItem.NAME_EXOME_EXPRESS,
                "testQuoteId");
        Product dummyProduct = createDummyProduct();
        dummyProduct.setPrimaryPriceItem(priceItem);
        ProductOrderSample sample = new ProductOrderSample("SM-1234", BSPSampleDTO.DUMMY);
        ProductOrder order = new ProductOrder( TEST_CREATOR, "title",
                Collections.singletonList(sample), "quote", dummyProduct,
                createDummyResearchProject());

        order.updateAddOnProducts(Collections.singletonList(createDummyProduct()));
        return order;
    }

    public static Product createDummyProduct() {
        return new Product("productName", new ProductFamily("ProductFamily"), "description",
            "partNumber", new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements", "deliverables",
            true, "Exome Express", false);
    }


    public static ResearchProject createDummyResearchProject() {
        ResearchProject researchProject = new ResearchProject(10950L, "MyResearchProject", "To study stuff.", ResearchProject.IRB_ENGAGED);

        Set<Funding> fundingList = Collections.singleton(new Funding(Funding.PURCHASE_ORDER, "A piece of Funding", "POFunding"));
        researchProject.populateFunding(fundingList);
        researchProject.addFunding(new ResearchProjectFunding (researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        Collection<Irb> irbs = Collections.singleton(new Irb("irbInitial", ResearchProjectIRB.IrbType.FARBER));
        researchProject.populateIrbs(irbs);
        researchProject.addIrbNumber(new ResearchProjectIRB (researchProject, ResearchProjectIRB.IrbType.BROAD, "irb123"));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.OTHER, "irb456"));

        researchProject.addPerson( RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);
        researchProject.addPerson( RoleType.BROAD_PI, 10950L);
        researchProject.addPerson(RoleType.BROAD_PI, 10951L);
        return researchProject;
    }

}
