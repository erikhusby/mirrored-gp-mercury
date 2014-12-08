package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
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
import java.util.List;
import java.util.Map;

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
        Map<String, MercurySample> allMercurySamples = mercurySampleDao.findMapIdToMercurySample(sampleNames);
        Map<String, MercurySample.MetadataSource> metadataSources =
                sampleDataSourceResolver.resolve(sampleNames, allMercurySamples);

        List<String> bspSampleIds = new ArrayList<>();
        Collection<MercurySample> mercurySamples = new ArrayList<>();
        for (String sampleName : sampleNames) {
            MercurySample.MetadataSource metadataSource = metadataSources.get(sampleName);
            switch (metadataSource) {
            case BSP:
                bspSampleIds.add(sampleName);
                break;
            case MERCURY:
                mercurySamples.add(allMercurySamples.get(sampleName));
                break;
            default:
                throw new IllegalStateException("Unknown sample data source: " + metadataSource);
            }
        }
        Map<String, SampleData> sampleData = new HashMap<>();
        if (!bspSampleIds.isEmpty()) {
            Map<String, BspSampleData> bspSampleData = bspSampleDataFetcher.fetchSampleData(bspSampleIds);
            sampleData.putAll(bspSampleData);
        }
        sampleData.putAll(mercurySampleDataFetcher.fetchSampleData(mercurySamples));
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
        Map<String, MercurySample> allMercurySamples = mercurySampleDao.findMapIdToMercurySample(aliquotIds);

        Collection<MercurySample> mercurySamplesWithMercurySource = new ArrayList<>();
        Collection<String> sampleIdsWithBspSource = new ArrayList<>();

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
}
