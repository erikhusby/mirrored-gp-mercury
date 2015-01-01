package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import com.google.common.collect.Multimap;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM;
import static org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry.PriceItemType.REPLACEMENT_PRICE_ITEM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 * Test the progress object.
 */
@Test(groups = TestGroups.STUBBY, enabled = true)
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

        Assert.assertNotNull(firstAbandonedPDOKey, "The fetcher returned zero PDOs with abandoned samples");

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

        Assert.assertNotNull(firstCompletePDOKey, "The fetcher returned zero PDOs with completed samples");

        ProductOrder order = pdoDao.findByBusinessKey(firstCompletePDOKey);
        ProductOrderCompletionStatus realStatus = calculateStatus(order.getSamples());

        Assert.assertEquals(
                fetcherPercentComplete, realStatus.getPercentCompleted(),
                "Fetcher calculated different complete percentage");
    }

    private static ProductOrderCompletionStatus calculateStatus(List<ProductOrderSample> samples) {
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

    public void testGetStatusWithSplit() throws Exception {
        List<ProductOrder> allOrders = pdoDao.findAll();

        List<Long> allOrderIds = new ArrayList<>();
        for (ProductOrder order : allOrders) {
            allOrderIds.add(order.getProductOrderId());
        }

        // Duplicate the number of items to pad out the query list.
        allOrderIds.addAll(allOrderIds);

        // Make sure we have enough PDOs to test that the Splitter API is being used correctly.
        // This is also why we don't use getAllProgress() here instead.
        Assert.assertTrue(allOrderIds.size() > BaseSplitter.DEFAULT_SPLIT_SIZE);

        CompletionStatusFetcher fetcher = new CompletionStatusFetcher();
        fetcher.loadProgress(pdoDao, allOrderIds);

        // Need to use x2 here because of the list duplication above.
        Assert.assertEquals(fetcher.getKeys().size() * 2, allOrderIds.size(), "There should be statuses for every item");
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

        Assert.assertNotNull(firstCompleteAndAbandonedKey, "The fetcher returned zero PDOs with completed samples");

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

        Assert.assertNotNull(pdoWithSamplesKey, "The fetcher returned zero PDOs with any samples");

        ProductOrder order = pdoDao.findByBusinessKey(pdoWithSamplesKey);
        Assert.assertEquals(
                numSamples, order.getSamples().size(), "Fetcher calculated different number of samples");
    }


    /**
     * Utility method to extract samples which are unique by name within a Product Order from a {@link Multimap}.
     */
    private static ProductOrderSample getSample(Multimap<String, ProductOrderSample> multimap, String sampleName) {
        return multimap.get(sampleName).iterator().next();
    }


    /**
     * Utility method to set delivery status into samples which are unique by name within a Product Order.
     */
    private static void setDeliveryStatus(Multimap<String, ProductOrderSample> multimap, String sampleName,
                                          ProductOrderSample.DeliveryStatus deliveryStatus) {
        getSample(multimap, sampleName).setDeliveryStatus(deliveryStatus);
    }


    /**
     * Test over GPLIM-1206.
     */
    public void testCompletionPercentageWithSomeAbandonedSamples() {
        // Two samples are Abandoned, one is in Progress.
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(pdoDao, "SM-001A", "SM-002A", "SM-001P");
        Multimap<String, ProductOrderSample> samplesMultimap =
                ProductOrderTestFactory.groupBySampleId(productOrder);

        setDeliveryStatus(samplesMultimap, "SM-001P", ProductOrderSample.DeliveryStatus.NOT_STARTED);
        setDeliveryStatus(samplesMultimap, "SM-001A", ProductOrderSample.DeliveryStatus.ABANDONED);
        setDeliveryStatus(samplesMultimap, "SM-002A", ProductOrderSample.DeliveryStatus.ABANDONED);

        pdoDao.flush();
        pdoDao.clear();

        allPDOFetcher = new CompletionStatusFetcher();
        allPDOFetcher.loadProgress(pdoDao);

        String businessKey = productOrder.getBusinessKey();
        assertThat(allPDOFetcher.getPercentCompleted(businessKey), is(equalTo(0)));
        assertThat(allPDOFetcher.getPercentAbandoned(businessKey), is(greaterThanOrEqualTo(66)));
        assertThat(allPDOFetcher.getPercentInProgress(businessKey), is(equalTo(33)));
    }
}
