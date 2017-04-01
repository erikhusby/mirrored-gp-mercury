package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.mockito.Mockito;

public class ProductTestUtils {
    public static void addToMockPriceListCache(Product productForCache, PriceListCache mockPriceListCache, String price,
                                               String quoteId) throws QuoteNotFoundException, QuoteServerException {
        final QuotePriceItem testQuotePriceItem =
                new QuotePriceItem(productForCache.getPrimaryPriceItem().getCategory(),
                        productForCache.getPrimaryPriceItem().getName(),
                        productForCache.getPrimaryPriceItem().getName(), price, "test",
                        productForCache.getPrimaryPriceItem().getPlatform());

        Mockito.when(mockPriceListCache.findByKeyFields(productForCache.getPrimaryPriceItem().getPlatform(),
                productForCache.getPrimaryPriceItem().getCategory(),
                productForCache.getPrimaryPriceItem().getName())).thenReturn(testQuotePriceItem);

        Mockito.when(mockPriceListCache.findByKeyFields(productForCache.getPrimaryPriceItem())).thenReturn(testQuotePriceItem);
    }
}
