package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/10/12
 * Time: 2:20 PM
 */
@Test(enabled = false)
public class ProductOrderSampleDaoTest  extends ContainerTest {

    @Inject
    ProductOrderSampleDao productOrderSampleDao;

    @Inject
    ProductOrderDao productOrderDao;

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrderSample_";

    @Test
    public void testFindByProductOrder() throws Exception {

        // TODO hmc Need to test Dao method
        // create a productOrder and persist it.
//        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
//        List<ProductOrderSample> sampleList = new ArrayList<ProductOrderSample>();
//        sampleList.add(new ProductOrderSample("MS-1111"));
//        sampleList.add(new ProductOrderSample("MS-2222"));
//        sampleList.add(new ProductOrderSample("MS-3333"));
//        ProductOrder newProductOrder = new ProductOrder(testProductOrderTitle, sampleList, "quoteId", null, null );
//
//
//        // Try to find the list of samples for a given product Order
//        List<ProductOrderSample> productOrderSamplesFromDb = productOrderSampleDao.findByProductOrder(newProductOrder);
//        Assert.assertNotNull(productOrderSamplesFromDb);
//        Assert.assertEquals( productOrderSamplesFromDb.size(), 3);

    }

}
