package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SampleDataFetcher implements Serializable {

    @Inject
    private SampleDataSourceResolver sampleDataSourceResolver;

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Inject
    private MercurySampleDataFetcher mercurySampleDataFetcher;

    @Inject
    private MercurySampleDao mercurySampleDao;

    public SampleDataFetcher() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspSampleDataFetcher fetcher for fetching sample data from BSP
     */
    public SampleDataFetcher(@Nonnull BSPSampleDataFetcher bspSampleDataFetcher) {
        this.bspSampleDataFetcher = bspSampleDataFetcher;
    }

    public SampleDataFetcher(@Nonnull MercurySampleDao mercurySampleDao,
                             @Nonnull SampleDataSourceResolver sampleDataSourceResolver,
                             @Nonnull BSPSampleDataFetcher bspSampleDataFetcher,
                             @Nonnull MercurySampleDataFetcher mercurySampleDataFetcher) {
        this.mercurySampleDao = mercurySampleDao;
        this.sampleDataSourceResolver = sampleDataSourceResolver;
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.mercurySampleDataFetcher = mercurySampleDataFetcher;
    }

    public SampleDataFetcher(@Nonnull BSPSampleSearchService service) {
        this(service, null);
    }

    public SampleDataFetcher(@Nonnull BSPSampleSearchService service, @Nullable BSPConfig bspConfig) {
        this(new BSPSampleDataFetcher(service, bspConfig));
    }

    /**
     * Fetch the data for the given sample.
     *
     * @param sampleName The sample name.
     *
     * @return The sample DTO that was fetched.
     */
    public SampleData fetchSampleData(String sampleName) {
        return fetchSampleData(Collections.singleton(sampleName)).get(sampleName);
    }

    /**
     * Fetch the data for multiple samples.
     *
     * @param sampleNames The sample names, which should be short barcodes such as SM-4FHTK
     *
     * @return Mapping of sample id to its sample data
     */
    public Map<String, SampleData> fetchSampleData(@Nonnull Collection<String> sampleNames) {

        Collection<String> sampleIdsWithBspSource = new ArrayList<>();
        Collection<MercurySample> mercurySamplesWithMercurySource = new ArrayList<>();

        buildSampleCollectionsBySource(sampleNames, mercurySamplesWithMercurySource, sampleIdsWithBspSource);

        Map<String, SampleData> sampleData = new HashMap<>();
        if (!sampleIdsWithBspSource.isEmpty()) {
            Map<String, BspSampleData> bspSampleData = bspSampleDataFetcher.fetchSampleData(sampleIdsWithBspSource);
            sampleData.putAll(bspSampleData);
        }
        sampleData.putAll(mercurySampleDataFetcher.fetchSampleData(mercurySamplesWithMercurySource));
        return sampleData;
    }

    public void fetchFFPEDerived(@Nonnull Collection<SampleData> sampleDataCollection) {
        /*
         * FFPE is (for now) irrelevant for samples whose sample data is in Mercury. Therefore, only fetch FFPE-derived
         * for BspSampleData.
         */
        Collection<BspSampleData> bspSampleDataCollection = new ArrayList<>();
        for (SampleData sampleData : sampleDataCollection) {
            if (sampleData.getMetadataSource() == MercurySample.MetadataSource.BSP){
                bspSampleDataCollection.add((BspSampleData) sampleData);
            }
        }
        bspSampleDataFetcher.fetchFFPEDerived(bspSampleDataCollection);
    }

    /**
     * Given an aliquot ID, return its stock sample ID.
     */
    public String getStockIdForAliquotId(@Nonnull String aliquotId) {
        Map<String, String> stockIdByAliquotId = getStockIdByAliquotId(Collections.singleton(aliquotId));
        if (!stockIdByAliquotId.isEmpty()) {
            return stockIdByAliquotId.get(aliquotId);
        }
        return null;
    }

    /**
     * Given a list of aliquot IDs, return a map of aliquot IDs to stock IDs.
     */
    public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {

        Collection<MercurySample> mercurySamplesWithMercurySource = new ArrayList<>();
        Collection<String> sampleIdsWithBspSource = new ArrayList<>();

        buildSampleCollectionsBySource(aliquotIds, mercurySamplesWithMercurySource, sampleIdsWithBspSource);

        Map<String, String> stockIdByAliquotId = new HashMap<>();
        if (!mercurySamplesWithMercurySource.isEmpty()) {
            stockIdByAliquotId.putAll(mercurySampleDataFetcher.getStockIdByAliquotId(mercurySamplesWithMercurySource));
        }
        if (!sampleIdsWithBspSource.isEmpty()) {
            stockIdByAliquotId.putAll(bspSampleDataFetcher.getStockIdByAliquotId(sampleIdsWithBspSource));
        }

        return stockIdByAliquotId;
    }

    /**
     * Returns a map of barcode to SampleDetails object for each input barcode, which can be a manufacturer barcode
     * or SM-id barcode.
     */
    public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByBarcode(@Nonnull Collection<String> barcodes) {
        return bspSampleDataFetcher.fetchSampleDetailsByBarcode(barcodes);
    }

    private void buildSampleCollectionsBySource(Collection<String> aliquotIds,
                                                Collection<MercurySample> mercurySamplesWithMercurySource,
                                                Collection<String> sampleIdsWithBspSource) {
        Map<String, MercurySample> allMercurySamples = mercurySampleDao.findMapIdToMercurySample(aliquotIds);
        Map<String, MercurySample.MetadataSource> sourceBySampleId =
                sampleDataSourceResolver.resolve(aliquotIds, allMercurySamples);
        for (Map.Entry<String, MercurySample.MetadataSource> entry : sourceBySampleId.entrySet()) {
            String sampleId = entry.getKey();
            MercurySample.MetadataSource metadataSource = entry.getValue();

            switch (metadataSource) {
            case MERCURY:
                mercurySamplesWithMercurySource.add(allMercurySamples.get(sampleId));
                break;
            case BSP:
                sampleIdsWithBspSource.add(sampleId);
                break;
            default:
                throw new IllegalStateException(
                        String.format("Unknown sample data source %s for sample %s", metadataSource, sampleId));
            }
        }
    }

    /**
     * Fetch the SampleData for multiple product order samples. For cases where only a few pieces of data are needed for
     * each sample, a list of result properties can be specified as a hint to help optimize the data fetch. This is
     * helpful for performance when fetching sample data from BSP.
     *
     * Implementation note: The result properties are currently of type {@link BSPSampleSearchColumn}, which leaks some
     * details of the BSP fetch through the SampleDataFetcher abstraction. This could be made more general to clean up
     * the API. This should also involve an analysis of the overlap between BSPSampleSearchColumn and
     * {@link Metadata.Key}.
     *
     * @param samples                collection of product order sample for which sample data is needed
     * @param bspSampleSearchColumns hint for which columns to return data for, if performance is a factor
     *
     * @return Mapping of sample id to its sample data
     */
    public Map<String, SampleData> fetchSampleDataForProductOrderSamples(Collection<ProductOrderSample> samples,
                                                                         BSPSampleSearchColumn... bspSampleSearchColumns) {
        Map<String, SampleData> sampleData = new HashMap<>();

        Collection<MercurySample> mercurySamplesWithMercurySource = new ArrayList<>();
        Collection<String> sampleIdsWithBspSource = new ArrayList<>();

        Set<String> sampleNames = new HashSet<>(samples.size());
        for (ProductOrderSample productOrderSample : samples) {
            if (productOrderSample.needsBspMetaData()) {
                MercurySample mercurySample = productOrderSample.getMercurySample();
                if (mercurySample != null &&
                    mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    mercurySamplesWithMercurySource.add(mercurySample);
                } else {
                    sampleNames.add(productOrderSample.getName());
                }
            }
        }
        if (!sampleNames.isEmpty()) {

            buildSampleCollectionsBySource(sampleNames, mercurySamplesWithMercurySource, sampleIdsWithBspSource);

            if (!sampleIdsWithBspSource.isEmpty()) {
                Map<String, BspSampleData> bspSampleData =
                        bspSampleDataFetcher.fetchSampleData(sampleIdsWithBspSource, bspSampleSearchColumns);
                sampleData.putAll(bspSampleData);
            }
        }
        sampleData.putAll(mercurySampleDataFetcher.fetchSampleData(mercurySamplesWithMercurySource));

        return sampleData;
    }

    /**
     * Fetch the SampleData for multiple mercury samples. For cases where only a few pieces of data are needed for
     * each sample, a list of result properties can be specified as a hint to help optimize the data fetch. This is
     * helpful for performance when fetching sample data from BSP.
     *
     * Implementation note: The result properties are currently of type {@link BSPSampleSearchColumn}, which leaks some
     * details of the BSP fetch through the SampleDataFetcher abstraction. This could be made more general to clean up
     * the API. This should also involve an analysis of the overlap between BSPSampleSearchColumn and
     * {@link Metadata.Key}.
     *
     * @param samples                collection of product order sample for which sample data is needed
     * @param bspSampleSearchColumns hint for which columns to return data for, if performance is a factor
     *
     * @return Mapping of sample id to its sample data
     */
    public Map<String, SampleData> fetchSampleDataForMercurySamples(Collection<MercurySample> samples,
                                                                         BSPSampleSearchColumn... bspSampleSearchColumns) {
        Map<String, SampleData> sampleData = new HashMap<>();

        Collection<MercurySample> mercurySamplesWithMercurySource = new ArrayList<>();
        Collection<String> sampleIdsWithBspSource = new ArrayList<>();

        Set<String> sampleNames = new HashSet<>(samples.size());
        for (MercurySample mercurySample : samples) {
            if (mercurySample.needsBspMetaData()) {
                if (mercurySample != null && mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                    mercurySamplesWithMercurySource.add(mercurySample);
                } else {
                    sampleNames.add(mercurySample.getSampleKey());
                }
            }
        }
        if (!sampleNames.isEmpty()) {

            buildSampleCollectionsBySource(sampleNames, mercurySamplesWithMercurySource, sampleIdsWithBspSource);

            if (!sampleIdsWithBspSource.isEmpty()) {
                Map<String, BspSampleData> bspSampleData =
                        bspSampleDataFetcher.fetchSampleData(sampleIdsWithBspSource, bspSampleSearchColumns);
                sampleData.putAll(bspSampleData);
            }
        }
        sampleData.putAll(mercurySampleDataFetcher.fetchSampleData(mercurySamplesWithMercurySource));

        return sampleData;
    }
}
