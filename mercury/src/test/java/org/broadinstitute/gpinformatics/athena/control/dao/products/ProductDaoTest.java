package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

@Test
public class ProductDaoTest extends ContainerTest {

    @Inject
    private ProductDao dao;


    public void testFindTopLevelProducts() {

        final List<Product> products = dao.findProducts(ProductDao.TopLevelProductsOnly.YES);
        Assert.assertNotNull(products);

        // make sure exex is in there
        for (Product product : products) {
            if ("EXOME_EXPRESS-2012.11.01".equals(product.getPartNumber())) {
                return;
            }
        }

        Assert.fail("Did not find Exome Express top-level product!");

        // needs a negative test for the absence of non top level products

    }


    public void testFindAvailableProducts() {

        final List<Product> products = dao.findProducts(ProductDao.AvailableProductsOnly.YES);
        Assert.assertNotNull(products);

        // make sure exex is in there
        for (Product product : products) {
            if ("EXOME_EXPRESS-2012.11.01".equals(product.getPartNumber())) {
                return;
            }
        }

        Assert.fail("Did not find Exome Express top-level product!");

        // needs a negative test for non-available products.  this will require test data with products having null
        // availability dates and availability dates in the future.

    }


    public void testFindAvailableTopLevelProducts() {

        final List<Product> products = dao.findProducts(ProductDao.AvailableProductsOnly.YES, ProductDao.TopLevelProductsOnly.NO);
        Assert.assertNotNull(products);

        // make sure exex is in there
        for (Product product : products) {
            if ("EXOME_EXPRESS-2012.11.01".equals(product.getPartNumber())) {
                return;
            }
        }

        Assert.fail("Did not find Exome Express top-level product!");

        // needs a negative test for non-available products.  this will require test data with products having null
        // availability dates and availability dates in the future.

    }



    public void testFindByPartNumber() {

        final Product product = dao.findByPartNumber("EXOME_EXPRESS-2012.11.01");
        Assert.assertNotNull(product);

        Assert.assertEquals(product.getPartNumber(), "EXOME_EXPRESS-2012.11.01");
        Assert.assertTrue(product.isTopLevelProduct());
        Assert.assertEquals(product.getWorkflowName(), "EXEX-WF-2012.11.01");

        // negative test
        Product nonexistentProduct = dao.findByPartNumber("NONEXISTENT PART!!!");
        Assert.assertNull(nonexistentProduct);

    }
}
