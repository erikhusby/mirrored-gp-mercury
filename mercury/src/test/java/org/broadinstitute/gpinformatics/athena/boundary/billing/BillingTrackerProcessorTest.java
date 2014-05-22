package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BillingTrackerProcessor}.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BillingTrackerProcessorTest {

    public static final String TEST_SAMPLE_ID = "SM-1";
    public static final String MESSAGE_PREFIX = "Sheet test, Row #0 ";

    private BillingTrackerProcessor processor;

    @BeforeMethod
    public void setUp() {
        // Set up test data.
        Product product = new Product();
        product.setPrimaryPriceItem(new PriceItem());

        ProductOrder order = new ProductOrder();
        order.addSample(new ProductOrderSample(TEST_SAMPLE_ID));

        // Set up mocks.
        ProductDao mockProductDao = mock(ProductDao.class);
        PriceListCache mockPriceListCache = mock(PriceListCache.class);
        ProductOrderDao mockProductOrderDao = mock(ProductOrderDao.class);

        when(mockProductDao.findByPartNumber(anyString())).thenReturn(product);
        when(mockProductOrderDao.findByBusinessKey(anyString())).thenReturn(order);

        // Create unit under test.
        processor = new BillingTrackerProcessor("test", null, mockProductDao, mockProductOrderDao, null,
                mockPriceListCache, false);
    }

    public void testProcessRowDetailsCurrentDate() {
        Date now = new Date();
        String nowString = new SimpleDateFormat("MM/dd/yyyy").format(now);
        Map<String, String> dataRow = makeDataRow(nowString);

        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getMessages(),
                not(hasItem(MESSAGE_PREFIX + BillingTrackerProcessor.makeCompletedDateFutureErrorMessage(
                        TEST_SAMPLE_ID, nowString))));
    }

    public void testProcessRowDetailsFutureDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String tomorrowString = new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime());

        Map<String, String> dataRow = makeDataRow(tomorrowString);

        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getMessages(),
                hasItem(MESSAGE_PREFIX + BillingTrackerProcessor.makeCompletedDateFutureErrorMessage(
                        TEST_SAMPLE_ID, tomorrowString)));
    }

    public void testProcessRowDetailsWorkCompleteMoreThanThreeMonthsAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -3);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String oldDateString = new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime());

        Map<String, String> dataRow = makeDataRow(oldDateString);

        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getWarnings(), hasItem(MESSAGE_PREFIX +
                                                    BillingTrackerProcessor.makeCompletedDateTooOldErrorMessage(
                                                            TEST_SAMPLE_ID, oldDateString)));
    }

    public void testProcessRowDetailsInvalidDate() {
        Map<String, String> dataRow = makeDataRow("Feb 31");

        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getMessages(), hasItem(MESSAGE_PREFIX +
                                                    BillingTrackerProcessor.makeCompletedDateInvalidMessage("Feb 31",
                                                            TEST_SAMPLE_ID, "Draft-null")));
    }

    private Map<String, String> makeDataRow(String workCompleteDate) {
        Map<String, String> dataRow = new HashMap<>();
        dataRow.put(BillingTrackerHeader.WORK_COMPLETE_DATE.getText(), workCompleteDate);
        dataRow.put(BillingTrackerHeader.SAMPLE_ID.getText(), TEST_SAMPLE_ID);
        dataRow.put("null\n"
                    + "Billed [null]\n"
                    + "Update Quantity To Update Quantity To", "1");
        return dataRow;
    }
}
