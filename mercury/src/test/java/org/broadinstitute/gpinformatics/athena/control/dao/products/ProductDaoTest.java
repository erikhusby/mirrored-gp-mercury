package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDaoTest.DateSpec.FUTURE;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDaoTest.DateSpec.NULL;
import static org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDaoTest.DateSpec.PAST;


@Test(groups = TestGroups.STUBBY)
@Dependent
public class ProductDaoTest extends StubbyContainerTest {

    public ProductDaoTest(){}

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


    public static Product createProduct(ProductFamilyDao productFamilyDao, PriceItemDao priceItemDao) {

        ProductFamily metagenomicsProductFamily = productFamilyDao.find("Metagenomics");

        final int DAYS = 24 * 60 * 60;

        final Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        String partNumber = ProductTestFactory.generateProductPartNumber();
        Product product = new Product(
                "Test Data",                               // product name
                metagenomicsProductFamily,                 // product family
                "test data ",                              // description
                partNumber,                                // part number
                yesterday.getTime(),                       // availability date
                null,                                      // discontinued date
                3 * DAYS,                                  // expected cycle time
                4 * DAYS,                                  // guaranteed cycle time
                192,                                       // samples per week
                96,                                        // min order size
                "dummy input requirements",                // input requirements
                "dummy deliverables",                      // deliverables
                false,                                     // top level product
                Workflow.NONE,
                false, "agg type");

        // we have some tests that call this method more than once so the price item compound key must be unique
        // across invocations
        UUID uuid1 = UUID.randomUUID();
        PriceItem priceItem1 = new PriceItem (uuid1.toString(), PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Standard Pony-" + uuid1);

        UUID uuid2 = UUID.randomUUID();
        PriceItem priceItem2 = new PriceItem (uuid2.toString(), PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Pony Express-" + uuid2);

        product.setPrimaryPriceItem(priceItem1);

        return product;
    }


    private Product createProduct() {
        return createProduct(productFamilyDao, priceItemDao);
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



        public Product createDatesAndAvailabilityProduct() {

            Product product = createProduct();

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

        final List<Product> products =
                dao.findProducts(ProductDao.Availability.CURRENT_OR_FUTURE, ProductDao.TopLevelOnly.YES, ProductDao.IncludePDMOnly.NO);
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

        System.out.println("Calling with " + datesAndAvailability.toString());

        Product product = datesAndAvailability.createDatesAndAvailabilityProduct();
        dao.persist(product);
        dao.flush();

        List<Product> products = dao.findProducts(ProductDao.Availability.CURRENT, ProductDao.TopLevelOnly.NO, ProductDao.IncludePDMOnly.NO);
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

        final List<Product> products =
                dao.findProducts(ProductDao.Availability.CURRENT_OR_FUTURE, ProductDao.TopLevelOnly.NO, ProductDao.IncludePDMOnly.NO);
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
