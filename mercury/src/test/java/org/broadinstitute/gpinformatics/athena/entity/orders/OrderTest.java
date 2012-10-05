package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/1/12
 * Time: 4:37 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class OrderTest {
    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testOrder() throws Exception {

        Product product = new Product( "productName", new ProductFamily("ProductFamily"), "description",
                "partNumber", new Date(), new Date(), 12345678, 123456, 100, "inputRequirements", "deliverables",
                true, "workflowName");

        PriceItem priceItem1 = new PriceItem(product, PriceItem.Platform.GP, PriceItem.Category.EXOME_SEQUENCING_ANALYSIS,
                            PriceItem.PriceItemName.EXOME_EXPRESS, "quoteServicePriceItemId");

        HashSet<BillableItem> billableItems = new HashSet<BillableItem>();
        BillableItem billableItem1 = new BillableItem( priceItem1, new BigDecimal("1") );
        billableItems.add(billableItem1);

        //TODO hmc To be completed commented out now for change of priority.
        /**
        List<BillableSample> orderSamples = SampleSheetTest.createSampleList(
                "SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D", billableItems ) ;

        SampleSheet sampleSheet = new SampleSheet(orderSamples);
        Order order = new Order("title", sampleSheet, "quoteId", product, "researchProjectName" );

        //TODO hmc Under construction
        Assert.assertEquals(order.getSampleSheet().getSamples().size(), 6);
        Assert.assertTrue(order.getSampleSheet().getSamples().get(0).getBillableItems().size() == 1);

         **/

    }


}
