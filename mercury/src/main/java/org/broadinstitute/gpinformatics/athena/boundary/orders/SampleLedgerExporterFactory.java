package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import javax.inject.Inject;
import java.util.List;

/**
 * A factory class for creating SampleLedgerExporter instances for a list of ProductOrders. Allows callers to not need
 * to be aware of SampleLedgerExporter's dependencies.
 */
public class SampleLedgerExporterFactory {

    private final PriceItemDao priceItemDao;

    private final BSPUserList bspUserList;

    private final PriceListCache priceListCache;

    private final WorkCompleteMessageDao workCompleteMessageDao;

    private final BSPSampleDataFetcher sampleDataFetcher;

    @Inject
    public SampleLedgerExporterFactory(
            PriceItemDao priceItemDao,
            BSPUserList bspUserList,
            PriceListCache priceListCache,
            WorkCompleteMessageDao workCompleteMessageDao,
            BSPSampleDataFetcher sampleDataFetcher) {
        this.priceItemDao = priceItemDao;
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.sampleDataFetcher = sampleDataFetcher;
    }

    /**
     * Creates a SampleLedgerExporter for the given product orders.
     *
     * @param productOrders    the product orders to include in the ledger
     * @return a new exporter
     */
    public SampleLedgerExporter makeExporter(List<ProductOrder> productOrders) {
        return new SampleLedgerExporter(priceItemDao, bspUserList, priceListCache, productOrders,
                workCompleteMessageDao, sampleDataFetcher);
    }
}
