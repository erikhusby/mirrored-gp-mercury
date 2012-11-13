package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.*;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDaoTest.DateSpec.*;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class ProductDaoTest extends ContainerTest {

    @Inject
    private ProductDao dao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private UserTransaction utx;


    public enum DateSpec {
        NULL,
        PAST,
        FUTURE
    }


    @DataProvider(name = "availability")
    public Object [][] datesAndAvailabilityDataProvider() {
        return new Object [][] {
                // availability dates must all be non-null and in the past
                {new DatesAndAvailability(NULL, NULL, false)},
                {new DatesAndAvailability(NULL, PAST, false)},
                {new DatesAndAvailability(NULL, FUTURE, false)},
                {new DatesAndAvailability(FUTURE, NULL, false)},
                {new DatesAndAvailability(FUTURE, PAST, false)},
                {new DatesAndAvailability(FUTURE, FUTURE, false)},
                // discontinued date must be null or in the future
                {new DatesAndAvailability(PAST, PAST, false)},
                {new DatesAndAvailability(PAST, NULL, true)},
                {new DatesAndAvailability(PAST, FUTURE, true)}
        };

    }


    private Product createProduct() {

        ProductFamily metagenomicsProductFamily =
                ProductDaoTest.this.productFamilyDao.find("Metagenomics");


        final int DAYS = 24 * 60 * 60;

        Product product = new Product(
                "Test Data",                               // product name
                metagenomicsProductFamily,                 // product family
                "test data ",                              // description
                "PN-ProductDaoTest-" + UUID.randomUUID(),  // part number
                Calendar.getInstance().getTime(),          // availability date
                Calendar.getInstance().getTime(),          // discontinued date
                3 * DAYS,                                  // expected cycle time
                4 * DAYS,                                  // guaranteed cycle time
                192,                                       // samples per week
                96,                                        // min order size
                "dummy input requirements",                // input requirements
                "dummy deliverables",                      // deliverables
                false,                                     // top level product
                "dummy price item id"                      // quote server price item id
                ,
                false);

        List<PriceItem> priceItems = priceItemDao.findAll();
        Assert.assertNotNull(priceItems);
        Assert.assertTrue(priceItems.size() > 0);
        product.setDefaultPriceItem(priceItems.get(0));
        product.addPriceItem(priceItems.get(0));

        return product;
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

            Product product = ProductDaoTest.this.createProduct();

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

            product.setAvailabilityDate(availableDate);
            product.setDiscontinuedDate(discontinuedDate);

            return product;

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

        Product topLevelProduct = createProduct();
        String topLevelPartNumber = topLevelProduct.getPartNumber();
        topLevelProduct.setTopLevelProduct(true);
        dao.persist(topLevelProduct);

        Product notTopLevelProduct = createProduct();
        String notTopLevelPartNumber = notTopLevelProduct.getPartNumber();
        notTopLevelProduct.setTopLevelProduct(false);
        dao.persist(notTopLevelProduct);
        dao.flush();

        final List<Product> products = dao.findProducts(ProductDao.TopLevelOnly.YES);
        Assert.assertNotNull(products);

        // make sure our top level is in there and our not top level is not
        boolean foundTopLevel = false;
        boolean foundNotTopLevel = false;
        for (Product product : products) {
            if (topLevelPartNumber.equals(product.getPartNumber())) {
                foundTopLevel = true;
            }
            else if (notTopLevelPartNumber.equals(product.getPartNumber())) {
                foundNotTopLevel = true;
            }
        }

        Assert.assertTrue(foundTopLevel, "Did not find top level product!");
        Assert.assertFalse(foundNotTopLevel, "Unexpectedly found not top level product!");

    }


    /**
     * Drive this from a data provider so we can roll back the transaction around each test scenario
     *
     * @param datesAndAvailability
     */
    @Test(dataProvider = "availability")
    public void testFindAvailableProducts(DatesAndAvailability datesAndAvailability) {

        Product product = datesAndAvailability.createProduct();
        dao.persist(product);
        dao.flush();

        List<Product> products = dao.findProducts(ProductDao.AvailableOnly.YES);
        // filter out non test data
        CollectionUtils.filter(products, new Predicate<Product>() {
            @Override
            public boolean evaluate(Product product) {
                return "Test Data".equals(product.getProductName());
            }
        });


        if (!datesAndAvailability.expectingAvailable) {
            Assert.assertEquals(products.size(), 0);
        } else {
            Assert.assertEquals(products.size(), 1, datesAndAvailability.toString());
            Assert.assertEquals(products.get(0), product, datesAndAvailability.toString());
        }
    }


    public void testFindPDMOnlyProducts() {

        Product pdmOnlyProduct = createProduct();
        String pdmOnlyPartNumber = pdmOnlyProduct.getPartNumber();
        pdmOnlyProduct.setPdmOrderableOnly(true);
        dao.persist(pdmOnlyProduct);

        Product notPDMOnlyProduct = createProduct();
        String notPDMOnlyPartNumber = notPDMOnlyProduct.getPartNumber();
        notPDMOnlyProduct.setPdmOrderableOnly(false);
        dao.persist(notPDMOnlyProduct);
        dao.flush();

        final List<Product> products = dao.findProducts(ProductDao.IncludePDMOnly.NO);
        Assert.assertNotNull(products);

        // make sure our top level is in there and our not top level is not
        boolean foundPDMOnly = false;
        boolean foundNotPDMOnly = false;
        for (Product product : products) {
            if (pdmOnlyPartNumber.equals(product.getPartNumber())) {
                foundPDMOnly = true;
            }
            else if (notPDMOnlyPartNumber.equals(product.getPartNumber())) {
                foundNotPDMOnly = true;
            }
        }

        Assert.assertTrue(foundNotPDMOnly, "Did not find expected non-PDM only product!");
        Assert.assertFalse(foundPDMOnly, "Unexpectedly found PDM only product!");
    }



    public void testFindByPartNumber() {

        Product product = createProduct();

        dao.persist(product);
        dao.flush();

        Product foundProduct = dao.findByPartNumber(product.getPartNumber());
        Assert.assertNotNull(foundProduct, "Product not found!");

        Product nonexistentProduct = dao.findByPartNumber("NONEXISTENT PART!!!");
        Assert.assertNull(nonexistentProduct, "Unexpectedly found product that shouldn't exist!");

    }
}
