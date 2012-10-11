package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/10/12
 * Time: 2:20 PM
 */
public class ProductOrderSampleDaoTest extends GenericDao {


    @Inject
    ProductOrderSampleDao productOrderSampleDao;

    public static final String TEST_ORDER_TITLE_PREFIX = "TestProductOrderSample_";

    @Test
    public void testFindByProductOrder() throws Exception {

        // create a productOrder and persist it.
        String testProductOrderTitle = TEST_ORDER_TITLE_PREFIX + UUID.randomUUID();
        List<ProductOrderSample> sampleList = new ArrayList<ProductOrderSample>();
        sampleList.add(new ProductOrderSample("MS-1111"));
        ProductOrder newProductOrder = new ProductOrder(testProductOrderTitle, sampleList, "quoteId", null, null );
        productOrderSampleDao.persist(newProductOrder);
        productOrderSampleDao.flush();
        productOrderSampleDao.clear();

        // Try to find the list of samples for a given product Order
        List<ProductOrderSample> productOrderSamplesFromDb = productOrderSampleDao.findByProductOrder(newProductOrder);
        Assert.assertNotNull(productOrderSamplesFromDb);

    }

}
