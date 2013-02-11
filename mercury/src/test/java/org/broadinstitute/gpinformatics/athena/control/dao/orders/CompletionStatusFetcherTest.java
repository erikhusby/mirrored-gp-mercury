package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

/**
 * Test the progress object
 *
 * @author hrafal
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class CompletionStatusFetcherTest extends ContainerTest {

    @Inject
    private UserTransaction utx;

    @Inject
    private ProductOrderDao pdoDao;

    private CompletionStatusFetcher allPDOFetcher;

    @BeforeMethod
    public void setUp() throws Exception {        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        // Get all statuses
        allPDOFetcher = new CompletionStatusFetcher();
        allPDOFetcher.setupProgress(pdoDao);
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testGetPercentAbandoned() throws Exception {
        // Find the first PDO with abandoned samples
        String firstAbandonedPDOKey = null;
        int fetcherPercentAbandoned = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            if (allPDOFetcher.getPercentAbandoned(pdoKey) > 0) {
                firstAbandonedPDOKey = pdoKey;
                fetcherPercentAbandoned = allPDOFetcher.getPercentAbandoned(pdoKey);
                break;
            }
        }

        if (firstAbandonedPDOKey == null) {
            Assert.fail("The fetcher returned zero PDOs with abandoned samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(firstAbandonedPDOKey);

        int realTotal = 0;
        int abandoned = 0;
        for (ProductOrderSample sample : order.getSamples()) {
            if (ProductOrderSample.DeliveryStatus.ABANDONED == sample.getDeliveryStatus()) {
                abandoned++;
            }

            realTotal++;
        }

        Assert.assertEquals(
            "Fetcher calculated different abandon percentage", fetcherPercentAbandoned, (abandoned * 100)/realTotal);
    }

    public void testGetPercentComplete() throws Exception {
        // Find the first PDO with abandoned samples
        String firstCompletePDOKey = null;
        int fetcherPercentComplete = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            if (allPDOFetcher.getPercentComplete(pdoKey) > 0) {
                firstCompletePDOKey = pdoKey;
                fetcherPercentComplete = allPDOFetcher.getPercentComplete(pdoKey);
                break;
            }
        }

        if (firstCompletePDOKey == null) {
            Assert.fail("The fetcher returned zero PDOs with abandoned samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(firstCompletePDOKey);

        int realTotal = 0;
        int complete = 0;
        for (ProductOrderSample sample : order.getSamples()) {
            if (!sample.getLedgerItems().isEmpty()) {
                complete++;
            }

            realTotal++;
        }

        Assert.assertEquals(
                "Fetcher calculated different complete percentage", fetcherPercentComplete, (complete * 100)/realTotal);
    }

    public void testGetPercentCompleteAndAbandoned() throws Exception {
        // Find the first PDO with abandoned samples
        String firstCompleteAndAbandonedKey = null;
        int fetcherPercentCompleteAndAbandoned = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            if (allPDOFetcher.getPercentComplete(pdoKey) > 0) {
                firstCompleteAndAbandonedKey = pdoKey;
                fetcherPercentCompleteAndAbandoned = allPDOFetcher.getPercentCompleteAndAbandoned(pdoKey);
                break;
            }
        }

        if (firstCompleteAndAbandonedKey == null) {
            Assert.fail("The fetcher returned zero PDOs with abandoned samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(firstCompleteAndAbandonedKey);

        int realTotal = 0;
        int completeAndAbandoned = 0;
        for (ProductOrderSample sample : order.getSamples()) {
            if (!sample.getLedgerItems().isEmpty()) {
                completeAndAbandoned++;
            } else if (ProductOrderSample.DeliveryStatus.ABANDONED == sample.getDeliveryStatus()) {
                completeAndAbandoned++;
            }

            realTotal++;
        }

        Assert.assertEquals(
                "Fetcher calculated different complete percentage",
                fetcherPercentCompleteAndAbandoned, (completeAndAbandoned * 100)/realTotal);
    }

    public void testGetNumberOfSamples() throws Exception {
        String pdoWithSamplesKey = null;
        int numSamples = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            numSamples = allPDOFetcher.getNumberOfSamples(pdoKey);
            if (numSamples > 0) {
                pdoWithSamplesKey = pdoKey;
                break;
            }
        }

        if (pdoWithSamplesKey == null) {
            Assert.fail("The fetcher returned zero PDOs with any samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(pdoWithSamplesKey);
        Assert.assertEquals(
                "Fetcher calculated different number of samples", numSamples, order.getSamples().size());
    }
}
