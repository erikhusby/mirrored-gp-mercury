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
    private SampleLedgerSpreadSheetWriter spreadSheetWriter;

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
        SortedMap<Product, List<List<String>>> sampleRowDataByProduct = new TreeMap<>();
        for (ProductOrder productOrder : productOrders) {
            Product product = productOrder.getProduct();
            List<List<String>> sampleRowDataForProduct = sampleRowDataByProduct.get(product);
            if (sampleRowDataForProduct == null) {
                sampleRowDataForProduct = new ArrayList<>();
                sampleRowDataByProduct.put(product, sampleRowDataForProduct);
            }
            sampleRowDataForProduct.addAll(gatherSampleRowData(productOrder));
        }
        return new SampleLedgerExporter(priceItemDao, bspUserList, priceListCache, productOrders,
                workCompleteMessageDao, sampleDataFetcher, appConfig, tableauConfig,
                spreadSheetWriter, sampleRowDataByProduct);
    }

    public List<List<String>> gatherSampleRowData(ProductOrder productOrder) {
        ArrayList<List<String>> sampleRowData = new ArrayList<>();
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            ArrayList<String> data = new ArrayList<>();
            data.add(productOrderSample.getSampleKey());
            data.add(productOrderSample.getBspSampleDTO().getCollaboratorsSampleName());
            data.add(productOrderSample.getBspSampleDTO().getMaterialType());
            data.add(productOrderSample.getRiskString());
            data.add(productOrderSample.getDeliveryStatus().getDisplayName());
            data.add(productOrderSample.getProductOrder().getProduct().getProductName());
            data.add(productOrderSample.getProductOrder().getBusinessKey());
            data.add(productOrderSample.getProductOrder().getTitle());
            data.add(bspUserList.getById(productOrderSample.getProductOrder().getCreatedBy()).getFullName());
            sampleRowData.add(data);
        }
        return sampleRowData;
    }
}
