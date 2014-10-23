package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
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
        Map<String, List<MercurySample>> allMercurySamples = mercurySampleDao.findMapIdToListMercurySample(sampleNames);
        Map<String, MercurySample.MetadataSource> metadataSources =
                sampleDataSourceResolver.resolveSampleDataSources(sampleNames, allMercurySamples);

        List<String> bspSampleIds = new ArrayList<>();
        Collection<MercurySample> mercurySamples = new ArrayList<>();
        for (String sampleName : sampleNames) {
            MercurySample.MetadataSource metadataSource = metadataSources.get(sampleName);
            switch (metadataSource) {
            case BSP:
                bspSampleIds.add(sampleName);
                break;
            case MERCURY:
                mercurySamples.addAll(allMercurySamples.get(sampleName));
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
            if (sampleData instanceof BspSampleData) {
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
        Map<String, String> stockIdByAliquotId = new HashMap<>();

        Map<MercurySample.MetadataSource, Collection<MercurySample>> samplesBySource =
                determineMetadataSource(aliquotIds);

        collectStockIdByAliquotIdForSamplesWithBspSource(stockIdByAliquotId, aliquotIds, samplesBySource);
        collectStockIdByAliquotIdForSamplesWithMercurySource(stockIdByAliquotId,
                samplesBySource.get(MercurySample.MetadataSource.MERCURY));

        return stockIdByAliquotId;
    }

    private void collectStockIdByAliquotIdForSamplesWithBspSource(
            Map<String, String> stockIdByAliquotId, Collection<String> aliquotIds,
            Map<MercurySample.MetadataSource, Collection<MercurySample>> samplesBySource) {
        Collection<String> sampleIdsWithKnownSource = new ArrayList<>();
        for (Collection<MercurySample> mercurySamples : samplesBySource.values()) {
            for (MercurySample mercurySample : mercurySamples) {
                sampleIdsWithKnownSource.add(mercurySample.getSampleKey());
            }
        }
        Collection<String> aliquotsWithNoMercurySample = CollectionUtils.subtract(aliquotIds, sampleIdsWithKnownSource);
        Collection<String> sampleIdsWithBspSource = new ArrayList<>(aliquotsWithNoMercurySample);
        Collection<MercurySample> mercurySamplesWithBspSource = samplesBySource.get(MercurySample.MetadataSource.BSP);
        if (mercurySamplesWithBspSource != null) {
            for (MercurySample mercurySample : mercurySamplesWithBspSource) {
                sampleIdsWithBspSource.add(mercurySample.getSampleKey());
            }
        }
        if (!sampleIdsWithBspSource.isEmpty()) {
            stockIdByAliquotId.putAll(bspSampleDataFetcher.getStockIdByAliquotId(sampleIdsWithBspSource));
        }
    }

    private void collectStockIdByAliquotIdForSamplesWithMercurySource(Map<String, String> stockIdByAliquotId,
                                                                      Collection<MercurySample> mercurySamples) {
        if (mercurySamples != null && !mercurySamples.isEmpty()) {
            stockIdByAliquotId.putAll(mercurySampleDataFetcher.getStockIdByAliquotId(mercurySamples));
        }
    }

    /**
     * Returns a map of barcode to SampleDetails object for each input barcode, which can be a manufacturer barcode
     * or SM-id barcode.
     */
    public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByBarcode(@Nonnull Collection<String> barcodes) {
        return bspSampleDataFetcher.fetchSampleDetailsByBarcode(barcodes);
    }

    /**
     * Given a Collection of sampleIds, return a Map of MercurySamples keyed on MetadataSource.
     */
    Map<MercurySample.MetadataSource, Collection<MercurySample>> determineMetadataSource(Collection<String> sampleIds) {
        Map<String, List<MercurySample>> mercurySamples = mercurySampleDao.findMapIdToListMercurySample(sampleIds);
        Map<String, MercurySample.MetadataSource> metadataSourceMap = sampleDataSourceResolver.resolveSampleDataSources(
                sampleIds, mercurySamples);
        Multimap<MercurySample.MetadataSource, MercurySample> results = HashMultimap.create();
        for (Map.Entry<String, MercurySample.MetadataSource> metadataSourceEntry : metadataSourceMap.entrySet()) {
            results.putAll(metadataSourceEntry.getValue(), mercurySamples.get(metadataSourceEntry.getKey()));
        }
        return results.asMap();
    }
}
