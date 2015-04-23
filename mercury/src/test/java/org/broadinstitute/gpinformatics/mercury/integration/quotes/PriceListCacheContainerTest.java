package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePlatformType;
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

        int crspItemCount = 0;
        int externalListCount = 0;

        for (QuotePriceItem quotePriceItem : quotePriceItems) {
            Assert.assertNotNull(quotePriceItem.getPlatformName(), quotePriceItem.toString());
            // category actually can be null
            // Assert.assertNotNull(quotePriceItem.getCategoryName(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getName(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getSubmittedDate(), quotePriceItem.toString());
            Assert.assertNotNull(quotePriceItem.getEffectiveDate(), quotePriceItem.toString());
            if(quotePriceItem.getPlatformName().equals(QuotePlatformType.CRSP.getPlatformName())) {
                crspItemCount++;
            } else if(quotePriceItem.getPlatformName().equals(QuotePlatformType.GSP.getPlatformName())) {
                externalListCount++;
            }
        }
        System.out.println("The crsp item count is " + crspItemCount);
        System.out.println("The external item count is " + externalListCount);
        Assert.assertTrue(crspItemCount > 0);
        Assert.assertTrue(externalListCount > 0);
    }
}
