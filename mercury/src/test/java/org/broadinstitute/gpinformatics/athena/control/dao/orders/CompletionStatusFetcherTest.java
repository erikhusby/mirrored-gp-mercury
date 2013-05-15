package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM;
import static org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM;

/**
 * Test the progress object.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class CompletionStatusFetcherTest extends ContainerTest {

    @Inject
    private ProductOrderDao pdoDao;

    private CompletionStatusFetcher allPDOFetcher;

    @BeforeMethod
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (pdoDao != null) {
            // Get all statuses.
            allPDOFetcher = new CompletionStatusFetcher();
            allPDOFetcher.loadProgress(pdoDao);
        }
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
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
            } else {
                for (LedgerEntry ledgerEntry : sample.getLedgerItems()) {
                    LedgerEntry.PriceItemType priceItemType = ledgerEntry.getPriceItemType();
                    // If we find a ledger entry with a priceItemType of PRIMARY_PRICE_ITEM or REPLACEMENT_PRICE_ITEM,
                    // this sample can be considered complete for the purposes of the CompletionStatusFetcher.
                    // If the priceItemType is ADD_ON_PRICE_ITEM or null (as would be the case for a Quote server
                    // error), we do not consider the sample complete.
                    if (PRIMARY_PRICE_ITEM.equals(priceItemType) || REPLACEMENT_PRICE_ITEM.equals(priceItemType)) {
                        completed++;
                        break;
                    }
                }
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
        int fetcherInProgress = 0;

        for (String pdoKey : allPDOFetcher.getKeys()) {
            if (allPDOFetcher.getPercentCompleted(pdoKey) > 0) {
                firstCompleteAndAbandonedKey = pdoKey;
                fetcherInProgress = allPDOFetcher.getPercentInProgress(pdoKey);
                break;
            }
        }

        if (firstCompleteAndAbandonedKey == null) {
            Assert.fail("The fetcher returned zero PDOs with abandoned samples");
        }

        ProductOrder order = pdoDao.findByBusinessKey(firstCompleteAndAbandonedKey);
        ProductOrderCompletionStatus realStatus = calculateStatus(order.getSamples());

        Assert.assertEquals(
                fetcherInProgress, realStatus.getPercentInProgress(),
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
