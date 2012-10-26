package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDaoTest.DateSpec.*;


@Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductDaoTest extends ContainerTest {

    @Inject
    private ProductDao dao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private UserTransaction utx;

    private List<Product> createdProducts;

    public enum DateSpec {
        NULL,
        PAST,
        FUTURE
    }

    private class DatesAndAvailability {

        private DateSpec availableDateSpec;

        private DateSpec discontinuedDateSpec;

        private boolean expectingAvailable;


        public DatesAndAvailability(DateSpec availableDateSpec, DateSpec discontinuedDateSpec, boolean expectingAvailable) {
            if (availableDateSpec == null) {
                throw new NullPointerException("available DateSpec can not be null!");
            }

            if (discontinuedDateSpec == null) {
                throw new NullPointerException("discontinued DateSpec can not be null!");
            }

            this.availableDateSpec = availableDateSpec;
            this.discontinuedDateSpec = discontinuedDateSpec;
            this.expectingAvailable = expectingAvailable;
        }


        public Product createProduct() {

            ProductFamily generalProductsProductFamily =
                    ProductDaoTest.this.productFamilyDao.find(ProductFamily.ProductFamilyName.GENERAL_PRODUCTS);

            final int DAYS = 24 * 60 * 60;

            Date availableDate = null;
            Date discontinuedDate = null;

            Calendar past = Calendar.getInstance();
            past.add(Calendar.YEAR, -1);

            Calendar future = Calendar.getInstance();
            future.add(Calendar.YEAR, 1);

            if (availableDateSpec == PAST) {
                availableDate = past.getTime();
            }
            else if (availableDateSpec == FUTURE) {
                availableDate = future.getTime();
            }

            if (discontinuedDateSpec == PAST) {
                discontinuedDate = past.getTime();
            }
            else if (discontinuedDateSpec == FUTURE) {
                discontinuedDate = future.getTime();
            }

            return new Product(
                    "Test Data",                        // product name
                    generalProductsProductFamily,       // product family
                    "test data ",                       // description
                    "dummy part number",                // part number
                    availableDate,                      // availability date
                    discontinuedDate,                   // discontinued date
                    3 * DAYS,                           // expected cycle time
                    4 * DAYS,                           // guaranteed cycle time
                    192,                                // samples per week
                    96,                                 // min order size
                    "dummy input requirements",         // input requirements
                    "dummy deliverables",               // deliverables
                    false,                              // top level product
                    "dummy price item id"               // quote server price item id
            );
        }

        @Override
        public String toString() {
            return "DatesAndAvailability{" +
                    "availableDateSpec=" + availableDateSpec +
                    ", discontinuedDateSpec=" + discontinuedDateSpec +
                    ", expectingAvailable=" + expectingAvailable +
                    '}';
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


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

        DatesAndAvailability [] datesAndAvailabilities = new DatesAndAvailability [] {
            // availability dates must all be non-null and in the past
            new DatesAndAvailability(NULL, NULL, false),
            new DatesAndAvailability(NULL, PAST, false),
            new DatesAndAvailability(NULL, FUTURE, false),
            new DatesAndAvailability(FUTURE, NULL, false),
            new DatesAndAvailability(FUTURE, PAST, false),
            new DatesAndAvailability(FUTURE, FUTURE, false),
            // discontinued date must be null or in the future
            new DatesAndAvailability(PAST, PAST, false),
            new DatesAndAvailability(PAST, NULL, true),
            new DatesAndAvailability(PAST, FUTURE, true)
        };

        createdProducts = new ArrayList<Product>();

        for (DatesAndAvailability datesAndAvailability : datesAndAvailabilities) {
            Product product = datesAndAvailability.createProduct();
            dao.persist(product);
            dao.flush();
            dao.clear();
            createdProducts.add(product);

            List<Product> products = dao.findProducts(ProductDao.AvailableProductsOnly.YES);
            // filter out non test data
            CollectionUtils.filter(products, new Predicate<Product>() {
                @Override
                public boolean evaluate(Product product) {
                    return "Test Data".equals(product.getProductName());
                }
            });


            if (! datesAndAvailability.expectingAvailable) {
                Assert.assertEquals(products.size(), 0);
            }
            else {
                Assert.assertEquals(products.size(), 1, datesAndAvailability.toString());
                Assert.assertEquals(products.get(0), product, datesAndAvailability.toString());
            }

            // merge product because it was detached because of previous dao.clear()
            product = dao.getEntityManager().merge(product);
            dao.remove(product);
            dao.flush();
            createdProducts.clear();
        }

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
