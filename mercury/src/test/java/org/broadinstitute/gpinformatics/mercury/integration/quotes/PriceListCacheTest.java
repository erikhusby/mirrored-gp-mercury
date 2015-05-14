package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.quote.*;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

/**
 * Database-free tests for PriceListCache.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class PriceListCacheTest {

    private static final Log log = LogFactory.getLog(PriceListCacheTest.class);
    
    @Test(groups = TestGroups.DATABASE_FREE)
    public void test_gsp_platform() {
        PriceList priceList = new PriceList();
        QuotePriceItem item1 = new QuotePriceItem("Illumina Sequencing","123","101bp MiSeq","5","Sample",QuotePlatformType.SEQ.getPlatformName());
        QuotePriceItem item2 = new QuotePriceItem("Illumina Sequencing","1234","151bp MiSeq","5","Sample",QuotePlatformType.SEQ.getPlatformName());
        priceList.add(item1);
        priceList.add(item2);
        priceList.add(new QuotePriceItem("Illumina Sequencing","1234","151bp MiSeq","3","Sample","Cookie Baking Platform"));

        PriceListCache cache = new PriceListCache(priceList.getQuotePriceItems());
        
        Collection<QuotePriceItem> quotePriceItems = cache.getGSPPriceItems();
        
        Assert.assertEquals(2, quotePriceItems.size());

        Assert.assertTrue(quotePriceItems.contains(item1));
        Assert.assertTrue(quotePriceItems.contains(item2));
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void test_gsp_prices() throws Exception {
        PriceListCache cache = new PriceListCache(new QuoteServiceStub().getAllPriceItems().getQuotePriceItems());
        Assert.assertFalse(cache.getGSPPriceItems().isEmpty());

        for (QuotePriceItem quotePriceItem : cache.getGSPPriceItems()) {
            Assert.assertTrue(QuotePlatformType.SEQ.getPlatformName().equalsIgnoreCase(quotePriceItem.getPlatformName()));
            log.debug(quotePriceItem.getName());
        }
    }
}
