package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
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

    @Inject
    public SampleLedgerExporterFactory(
            PriceItemDao priceItemDao,
            BSPUserList bspUserList,
            PriceListCache priceListCache) {
        this.priceItemDao = priceItemDao;
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;
    }

    /**
     * Creates a SampleLedgerExporter for the given product orders.
     *
     * @param productOrders    the product orders to include in the ledger
     * @return a new exporter
     */
    public SampleLedgerExporter makeExporter(List<ProductOrder> productOrders) {
        return new SampleLedgerExporter(priceItemDao, bspUserList, priceListCache, productOrders);
    }
}
