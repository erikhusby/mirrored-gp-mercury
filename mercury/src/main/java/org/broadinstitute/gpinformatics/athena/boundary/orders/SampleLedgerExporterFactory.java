package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.work.WorkCompleteMessageDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.tableau.TableauConfig;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A factory class for creating {@link SampleLedgerExporter} instances for a list of ProductOrders. Allows callers to
 * not need to be aware of SampleLedgerExporter's dependencies.
 * <p>
 * This factory is also gradually gaining the responsibility for gathering the data from entities and other sources,
 * thereby eliminating dependencies from SampleLedgerExporter and allowing it to be tested more easily. Eventually, this
 * factory will simply provide SampleLedgerExporter with a list of {@link SampleLedgerRow}s containing the data to
 * write to the spreadsheet.
 */
public class SampleLedgerExporterFactory {

    private final PriceItemDao priceItemDao;
    private final BSPUserList bspUserList;
    private final PriceListCache priceListCache;
    private final WorkCompleteMessageDao workCompleteMessageDao;
    private final BSPSampleDataFetcher sampleDataFetcher;
    private final AppConfig appConfig;
    private final TableauConfig tableauConfig;
    private final SampleLedgerSpreadSheetWriter spreadSheetWriter;

    @Inject
    public SampleLedgerExporterFactory(
            PriceItemDao priceItemDao,
            BSPUserList bspUserList,
            PriceListCache priceListCache,
            WorkCompleteMessageDao workCompleteMessageDao,
            BSPSampleDataFetcher sampleDataFetcher,
            AppConfig appConfig,
            TableauConfig tableauConfig,
            SampleLedgerSpreadSheetWriter spreadSheetWriter) {
        this.priceItemDao = priceItemDao;
        this.bspUserList = bspUserList;
        this.priceListCache = priceListCache;
        this.workCompleteMessageDao = workCompleteMessageDao;
        this.sampleDataFetcher = sampleDataFetcher;
        this.appConfig = appConfig;
        this.tableauConfig = tableauConfig;
        this.spreadSheetWriter = spreadSheetWriter;
    }

    /**
     * Creates a SampleLedgerExporter for the given product orders.
     *
     * @param productOrders    the product orders to include in the ledger
     * @return a new exporter
     */
    public SampleLedgerExporter makeExporter(List<ProductOrder> productOrders) {
        SortedMap<Product, List<SampleLedgerRow>> sampleRowDataByProduct = new TreeMap<>();
        for (ProductOrder productOrder : productOrders) {
            Product product = productOrder.getProduct();
            List<SampleLedgerRow> sampleRowDataForProduct = sampleRowDataByProduct.get(product);
            if (sampleRowDataForProduct == null) {
                sampleRowDataForProduct = new ArrayList<>();
                sampleRowDataByProduct.put(product, sampleRowDataForProduct);
            }
            sampleRowDataForProduct.addAll(gatherSampleRowData(productOrder));
        }
        return new SampleLedgerExporter(priceItemDao, priceListCache, productOrders, workCompleteMessageDao,
                sampleDataFetcher, appConfig, tableauConfig, spreadSheetWriter, sampleRowDataByProduct);
    }

    /**
     * Gathers the data for the samples in the given product order.
     *
     * @param productOrder    the product order to gather sample data for
     * @return a list of row data
     */
    public List<SampleLedgerRow> gatherSampleRowData(ProductOrder productOrder) {
        ArrayList<SampleLedgerRow> sampleRowData = new ArrayList<>();
        productOrder.loadBspData();
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            sampleRowData.add(new SampleLedgerRow(productOrderSample,
                    bspUserList.getUserFullName(productOrderSample.getProductOrder().getCreatedBy())));
        }
        return sampleRowData;
    }
}
