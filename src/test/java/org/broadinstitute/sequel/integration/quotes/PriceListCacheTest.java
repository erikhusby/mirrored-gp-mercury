package org.broadinstitute.sequel.integration.quotes;

import org.broadinstitute.sequel.infrastructure.quote.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class PriceListCacheTest {
    
    @Test(groups = DATABASE_FREE)
    public void test_gsp_platform() {
        PriceList priceList = new PriceList();
        PriceItem item1 = new PriceItem("Illumina Sequencing","123","101bp MiSeq","5","Sample",PriceItem.GSP_PLATFORM_NAME);
        PriceItem item2 = new PriceItem("Illumina Sequencing","1234","151bp MiSeq","5","Sample",PriceItem.GSP_PLATFORM_NAME);       
        priceList.add(item1);
        priceList.add(item2);
        priceList.add(new PriceItem("Illumina Sequencing","1234","151bp MiSeq","3","Sample","Cookie Baking Platform"));

        PriceListCache cache = new PriceListCache(priceList);
        
        Collection<PriceItem> priceItems = cache.getGSPPriceList();
        
        Assert.assertEquals(2, priceItems.size());

        Assert.assertTrue(priceItems.contains(item1));
        Assert.assertTrue(priceItems.contains(item2));
    }
    
    @Test(groups = EXTERNAL_INTEGRATION)
    public void test_gsp_prices() throws Exception {
        PriceListCache cache = new PriceListCache(new QuoteServiceStub().getAllPriceItems());
        Assert.assertFalse(cache.getGSPPriceList().isEmpty());

        for (PriceItem priceItem : cache.getGSPPriceList()) {
            Assert.assertTrue(PriceItem.GSP_PLATFORM_NAME.equalsIgnoreCase(priceItem.getPlatform()));
            System.out.println(priceItem.getName());
        }
    }
}
