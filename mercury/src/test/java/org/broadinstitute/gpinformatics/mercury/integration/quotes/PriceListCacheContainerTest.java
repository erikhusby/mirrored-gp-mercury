package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

@Test(groups = TestGroups.STANDARD)
public class PriceListCacheContainerTest {
    private PriceListCache priceListCache=new PriceListCache(QuoteServiceProducer.testInstance());

    @Test(groups = TestGroups.STANDARD)
    public void testSanity() {

        Assert.assertNotNull(priceListCache);

        Collection<QuotePriceItem> quotePriceItems = priceListCache.getQuotePriceItems();
        Assert.assertNotNull(quotePriceItems);
        Assert.assertTrue(quotePriceItems.size() > 10);

        for (QuotePriceItem quotePriceItem : quotePriceItems) {
            Assert.assertNotNull(quotePriceItem.getPlatformName(), quotePriceItem.toString());
            // category actually can be null
            // Assert.assertNotNull(quotePriceItem.getCategoryName(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getName(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getSubmittedDate(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getEffectiveDate(), quotePriceItem.toString());
        }
    }
}
