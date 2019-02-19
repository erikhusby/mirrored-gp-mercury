package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.List;


@Test(groups = {TestGroups.STUBBY})
@Dependent
public class PriceItemDaoTest extends StubbyContainerTest {

    public PriceItemDaoTest(){}

    @Inject
    private PriceItemDao dao;

    private Log log = LogFactory.getLog(this.getClass());

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;


    @BeforeMethod
    public void beforeMethod() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        createFixtureData();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


    private void createFixtureData() {
        PriceItem priceItem;

        priceItem = new PriceItem("1234", PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Standard Pony");
        dao.persist(priceItem);

        priceItem = new PriceItem("5678", PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Pony Express");
        dao.persist(priceItem);

        dao.flush();
    }


    public void testFindAll() {

        final List<PriceItem> priceItems = dao.findAll();
        Assert.assertNotNull(priceItems);

        Assert.assertTrue(priceItems.size() > 1);

        for (PriceItem priceItem : priceItems) {

            log.warn("Price Item name: " + priceItem.getName());
            if ( "Pony Express".equals(priceItem.getName()) ) {
                return;
            }
        }

        Assert.fail("Express price item not found!");

    }


    public void testFind() {

        final PriceItem priceItem = dao.find(PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Standard Pony");
        Assert.assertNotNull(priceItem);

        // deliberately mismatching the name
        PriceItem missingPriceItem = dao.find(PriceItem.PLATFORM_GENOMICS, "Pony Genomics", "Stone Pony");
        Assert.assertNull(missingPriceItem);

    }

}
