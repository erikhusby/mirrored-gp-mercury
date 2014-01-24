package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

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
    private final AppConfig appConfig;
    private final TableauConfig tableauConfig;

    @Inject
    public SampleLedgerExporterFactory(
            PriceItemDao priceItemDao,
            BSPUserList bspUserList,
            PriceListCache priceListCache,
            WorkCompleteMessageDao workCompleteMessageDao,
            BSPSampleDataFetcher sampleDataFetcher,
            AppConfig appConfig,
            TableauConfig tableauConfig) {
        this.priceItemDao = priceItemDao;
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.appConfig = appConfig;
        this.tableauConfig = tableauConfig;
    }

    /**
     * Creates a SampleLedgerExporter for the given product orders.
     *
     * @param productOrders    the product orders to include in the ledger
     * @return a new exporter
     */
    public SampleLedgerExporter makeExporter(List<ProductOrder> productOrders) {
        return new SampleLedgerExporter(priceItemDao, bspUserList, priceListCache, productOrders,
                workCompleteMessageDao, sampleDataFetcher, appConfig, tableauConfig);
    }
}
