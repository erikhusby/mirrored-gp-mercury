package org.broadinstitute.gpinformatics.infrastructure.athena;

import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 4:47 PM
 */
@Stub
public class AthenaClientServiceStub implements AthenaClientService {

    @Override
    public ProductOrder retrieveProductOrderDetails ( String poBusinessKey ) {

        Map<String, ProductOrder> productOrderByBusinessKeyMap = new HashMap<String, ProductOrder>();

        ProductOrder testOrder1 = createDummyProductOrder();
        testOrder1.setJiraTicketKey(poBusinessKey);
        productOrderByBusinessKeyMap.put(poBusinessKey,testOrder1);

//        if(!productOrderByBusinessKeyMap.containsKey(poBusinessKey)) {
//            throw new IllegalStateException("The key " + poBusinessKey + " does not map to a known ProductOrder");
//        }

        return productOrderByBusinessKeyMap.get(poBusinessKey);
    }


    /*
        helper Methods to create test data.  Moved from Test cases to aid stub implementation
     */
    public static ProductOrder createDummyProductOrder() {
        PriceItem priceItem = new PriceItem(PriceItem.Platform.GP, PriceItem.Category.EXOME_SEQUENCING_ANALYSIS,
                                    PriceItem.Name.EXOME_EXPRESS, "testQuoteId");
        Product dummyProduct = AthenaClientServiceStub.createDummyProduct ();
        dummyProduct.addPriceItem(priceItem);
        ProductOrder order = new ProductOrder( 1111L, "title",
                new ArrayList<ProductOrderSample> (), "quote", dummyProduct,
                AthenaClientServiceStub.createDummyResearchProject());

        ProductOrderSample sample = new ProductOrderSample("SM-1234", order);
        sample.addBillableItem(new BillableItem (priceItem, new BigDecimal ("1")));
        order.setSamples( Collections.singletonList ( sample ));

        return order;
    }

    public static Product createDummyProduct() {
        return new Product("productName", new ProductFamily("ProductFamily"), "description",
            "partNumber", new Date(), new Date(), 12345678, 123456, 100, 96, "inputRequirements", "deliverables",
            true, "workflowName");
    }


    public static ResearchProject createDummyResearchProject() {
        ResearchProject researchProject = new ResearchProject(10950L, "MyResearchProject", "To study stuff.", ResearchProject.IRB_ENGAGED);

        researchProject.addFunding(new ResearchProjectFunding (researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        researchProject.addIrbNumber(new ResearchProjectIRB (researchProject, ResearchProjectIRB.IrbType.BROAD, "irb123"));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.OTHER, "irb456"));

        researchProject.addPerson( RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);
        return researchProject;
    }

}
