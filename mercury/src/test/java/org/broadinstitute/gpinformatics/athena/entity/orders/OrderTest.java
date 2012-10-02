package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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


        BillableItem billableItem1 = new BillableItem( "NewProductName1", BillingStatus.NotYetBilled, 1);
        BillableItem billableItem2 = new BillableItem( "NewProductName2", BillingStatus.NotYetBilled, 1);
        HashSet<BillableItem> billableItems = new HashSet<BillableItem>();
        billableItems.add(billableItem1);
        billableItems.add(billableItem2);

        List<AthenaSample> orderSamples = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                billableItems ) ;
        SampleSheet sampleSheet = new SampleSheet();
        sampleSheet.setSamples(orderSamples);
        Order order = new Order( "title", "researchProjectName", "barcode",  OrderStatus.Draft, "quoteId",
                "comments", sampleSheet) ;

        //TODO hmc Under construction
        Assert.assertEquals(order.getSampleSheet().getSamples().size(), 6);
//        Assert.assertEquals(order.getSampleSheet().areAllSampleBSPFormat(), true);
        Assert.assertTrue(order.getSampleSheet().getSamples().get(0).getBillableItems().size() == 2);

    }


    private List<AthenaSample>  createSampleList( String sampleListStr, HashSet<BillableItem> billableItems) {
        List<AthenaSample> orderSamples = new ArrayList<AthenaSample>();
        String [] sampleArray = sampleListStr.split(",");
        for ( String sampleId : sampleArray) {
            AthenaSample athenaSample = new AthenaSample();
            athenaSample.setComment("athenaComment");
            athenaSample.setSampleId(sampleId);
            athenaSample.setBillableItems( billableItems );
            orderSamples.add(athenaSample);
        }
        return orderSamples;
    }


}
