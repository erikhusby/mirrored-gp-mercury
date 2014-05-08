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
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BillingTrackerProcessorTest {

    private BillingTrackerProcessor processor;

    @BeforeMethod
    public void setUp() {
        // Set up test data.
        Product product = new Product();
        product.setPrimaryPriceItem(new PriceItem());

        ProductOrder order = new ProductOrder();
        order.addSample(new ProductOrderSample("SM-1"));

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
        assertThat(processor.getMessages(), not(hasItem(makeCompletedDateFutureErrorMessage(nowString))));
    }

    public void testProcessRowDetailsFutureDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String tomorrowString = new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime());

        Map<String, String> dataRow = makeDataRow(tomorrowString);

        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getMessages(), hasItem(makeCompletedDateFutureErrorMessage(tomorrowString)));
    }

    @Test(enabled = false)
    public void testProcessRowDetailsWorkCompleteMoreThanThreeMonthsAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -3);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String oldDateString = new SimpleDateFormat("MM/dd/yyyy").format(calendar.getTime());

        Map<String, String> dataRow = makeDataRow(oldDateString);

        processor.processRowDetails(dataRow, 0);
        assertThat(processor.getMessages(), hasItem(makeCompletedDateTooOldErrorMessage(oldDateString)));
    }

    private Map<String, String> makeDataRow(String workCompleteDate) {
        Map<String, String> dataRow = new HashMap<>();
        dataRow.put(BillingTrackerHeader.WORK_COMPLETE_DATE.getText(), workCompleteDate);
        dataRow.put(BillingTrackerHeader.SAMPLE_ID.getText(), "SM-1");
        dataRow.put("null\n"
                    + "Billed [null]\n"
                    + "Update Quantity To Update Quantity To", "1");
        return dataRow;
    }

    private String makeCompletedDateFutureErrorMessage(String nowString) {
        return String.format("Sheet test, Row #0 " + BillingTrackerProcessor.FUTURE_WORK_COMPLETE_DATE_MESSAGE, "SM-1",
                nowString);
    }

    private String makeCompletedDateTooOldErrorMessage(String nowString) {
        return String.format("Sheet test, Row #0 " + BillingTrackerProcessor.OLD_WORK_COMPLETE_DATE_MESSAGE, "SM-1",
                nowString);
    }
}
