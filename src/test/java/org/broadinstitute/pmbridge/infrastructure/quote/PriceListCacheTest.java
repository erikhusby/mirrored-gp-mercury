package org.broadinstitute.pmbridge.infrastructure.quote;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.broadinstitute.pmbridge.TestGroups.UNIT;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/21/12
 * Time: 2:30 PM
 */
public class PriceListCacheTest {

    @Test(groups = UNIT)
    public void test_gsp_platform() {
        PriceList priceList = new PriceList();
        PriceItem item1 = new PriceItem("Illumina Sequencing","123","101bp MiSeq","5","Sample", QuotePlatformType.SEQ.getPlatformName() );
        PriceItem item2 = new PriceItem("Illumina Sequencing","1234","151bp MiSeq","5","Sample",QuotePlatformType.SEQ.getPlatformName() );
        priceList.add(item1);
        priceList.add(item2);
        priceList.add(new PriceItem("Illumina Sequencing","1234","151bp MiSeq","3","Sample","Cookie Baking Platform"));

        PriceListCache cache = new PriceListCache(priceList);

        Collection<PriceItem> priceItems = cache.getPlatformPriceList(QuotePlatformType.SEQ.getPlatformName());

        Assert.assertEquals(2, priceItems.size());

        Assert.assertTrue(priceItems.contains(item1));
        Assert.assertTrue(priceItems.contains(item2));
    }

    @Test(groups = UNIT)
    public void test_gsp_prices() throws Exception {
        PriceListCache cache = new PriceListCache(new MockQuoteServiceImpl().getAllPriceItems());
        Assert.assertFalse(cache.getPlatformPriceList(QuotePlatformType.SEQ.getPlatformName()).isEmpty());

        for (PriceItem priceItem : cache.getPlatformPriceList(QuotePlatformType.SEQ.getPlatformName()) ) {
            Assert.assertTrue(QuotePlatformType.SEQ.getPlatformName().equalsIgnoreCase(priceItem.getPlatform()));
            System.out.println(priceItem.getName());
        }
    }
}
