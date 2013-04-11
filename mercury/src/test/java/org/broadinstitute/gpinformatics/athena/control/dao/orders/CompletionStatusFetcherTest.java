package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test the progress object.
 *
 * @author hrafal
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class CompletionStatusFetcherTest extends ContainerTest {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @Inject
    private ProductOrderDao pdoDao;

    private CompletionStatusFetcher allPDOFetcher;

    @BeforeMethod
    public void setUp() throws Exception {        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        // Get all statuses
        allPDOFetcher = new CompletionStatusFetcher();
        allPDOFetcher.loadProgress(pdoDao);
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testGetPercentAbandoned() throws Exception {
        // Find the first PDO with abandoned samples.
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
        ProductOrderCompletionStatus realStatus = calculateStatus(order.getSamples());

        Assert.assertEquals(
            fetcherPercentAbandoned, realStatus.getPercentAbandoned(),
            "Fetcher calculated different abandon percentage");
    }

    public void testGetPercentComplete() throws Exception {
        // Find the first PDO with abandoned samples.
        String firstCompletePDOKey = null;
        int fetcherPercentComplete = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            if (allPDOFetcher.getPercentCompleted(pdoKey) > 0) {
                firstCompletePDOKey = pdoKey;
                fetcherPercentComplete = allPDOFetcher.getPercentCompleted(pdoKey);
                break;
            }
        }

        if (firstCompletePDOKey == null) {
            Assert.fail("The fetcher returned zero PDOs with abandoned samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(firstCompletePDOKey);
        ProductOrderCompletionStatus realStatus = calculateStatus(order.getSamples());

        Assert.assertEquals(
                fetcherPercentComplete, realStatus.getPercentCompleted(),
                "Fetcher calculated different complete percentage");
    }

    private ProductOrderCompletionStatus calculateStatus(List<ProductOrderSample> samples) {
        int total = 0;
        int completed = 0;
        int abandoned = 0;
        for (ProductOrderSample sample : samples) {
            if (sample.getDeliveryStatus() == ProductOrderSample.DeliveryStatus.ABANDONED) {
                abandoned++;
            } else if (!sample.getLedgerItems().isEmpty()) {
                completed++;
            }

            total++;
        }

        return new ProductOrderCompletionStatus(abandoned, completed, total);
    }

    public void testGetAllStatuses() throws Exception {
        List<ProductOrder> allOrders = pdoDao.findAll();

        List<String> allBusinessKeys = new ArrayList<String>();
        for (ProductOrder order : allOrders) {
            if (order.hasJiraTicketKey()) {
                allBusinessKeys.add(order.getBusinessKey());
            }
        }

        allBusinessKeys.addAll(allBusinessKeys);

        Assert.assertTrue(allBusinessKeys.size() > 1000);

        Map<String, ProductOrderCompletionStatus> statusMap = pdoDao.getProgressByBusinessKey(allBusinessKeys);

        Assert.assertEquals(statusMap.size() * 2, allBusinessKeys.size(), "There should be statuses for every item");
    }

    public void testGetPercentInProgress() throws Exception {
        // Find the first PDO with abandoned samples.
        String firstCompleteAndAbandonedKey = null;
        int fetcherInProgess = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            if (allPDOFetcher.getPercentCompleted(pdoKey) > 0) {
                firstCompleteAndAbandonedKey = pdoKey;
                fetcherInProgess = allPDOFetcher.getPercentInProgress(pdoKey);
                break;
            }
        }

        if (firstCompleteAndAbandonedKey == null) {
            Assert.fail("The fetcher returned zero PDOs with abandoned samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(firstCompleteAndAbandonedKey);
        ProductOrderCompletionStatus realStatus = calculateStatus(order.getSamples());

        Assert.assertEquals(
                fetcherInProgess, realStatus.getPercentInProgress(),
                "Fetcher calculated different in progress percentage");
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
                numSamples, order.getSamples().size(), "Fetcher calculated different number of samples");
    }
}
