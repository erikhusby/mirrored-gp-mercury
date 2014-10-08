package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
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
                             @Nonnull BSPSampleDataFetcher bspSampleDataFetcher,
                             @Nonnull MercurySampleDataFetcher mercurySampleDataFetcher) {
        this.mercurySampleDao = mercurySampleDao;
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
                determineMetadataSource(sampleNames, allMercurySamples);

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

    public void fetchFFPEDerived(@Nonnull Collection<SampleData> sampleDatas) {
        bspSampleDataFetcher.fetchFFPEDerived(BSPSampleDataFetcher.convertToBspSampleDataCollection(sampleDatas));
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
        Collection<String> allMercurySamples = new ArrayList<>();

        Map<MercurySample.MetadataSource, Collection<MercurySample>> samplesBySource =
                determineMetadataSource(aliquotIds);
        for (Collection<MercurySample> mercurySamples : samplesBySource.values()) {
            for (MercurySample mercurySample : mercurySamples) {
                allMercurySamples.add(mercurySample.getSampleKey());
            }
        }

        // Assume that BSP is the owner of samples data for sample IDs with no MercurySample.
        Collection<String> aliquotsWithNoMercurySample = CollectionUtils.subtract(aliquotIds, allMercurySamples);
        List<String> bspSampleIds = new ArrayList<>(aliquotsWithNoMercurySample);
        Map<String, String> stockIdByAliquotId = new HashMap<>();

        for (Map.Entry<MercurySample.MetadataSource, Collection<MercurySample>> entry : samplesBySource.entrySet()) {
            MercurySample.MetadataSource metadataSource = entry.getKey();
            if (metadataSource == MercurySample.MetadataSource.BSP) {
                for (MercurySample mercurySample : entry.getValue()) {
                    bspSampleIds.add(mercurySample.getSampleKey());
                }
                stockIdByAliquotId.putAll(bspSampleDataFetcher.getStockIdByAliquotId(bspSampleIds));
            } else if (metadataSource == MercurySample.MetadataSource.MERCURY) {
                stockIdByAliquotId.putAll(mercurySampleDataFetcher.getStockIdByAliquotId(entry.getValue()));
            } else {
                throw new IllegalStateException("Unknown sample data source: " + samplesBySource.keySet());
            }
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

    /**
     * Given a Collection of sampleIds, return a Map of MercurySamples keyed on MetadataSource.
     */
    Map<MercurySample.MetadataSource, Collection<MercurySample>> determineMetadataSource(Collection<String> sampleIds) {
        Map<String, List<MercurySample>> mercurySamples = mercurySampleDao.findMapIdToListMercurySample(sampleIds);
        Map<String, MercurySample.MetadataSource> metadataSourceMap = determineMetadataSource(sampleIds, mercurySamples);
        Map<MercurySample.MetadataSource, Collection<MercurySample>> results = new HashMap<>();
        for (Map.Entry<String, MercurySample.MetadataSource> metadataSourceEntry : metadataSourceMap.entrySet()) {
            results.put(metadataSourceEntry.getValue(), mercurySamples.get(metadataSourceEntry.getKey()));
        }
        return results;
    }

    @DaoFree
    private Map<String, MercurySample.MetadataSource> determineMetadataSource(Collection<String> sampleNames,
                                                                              Map<String, List<MercurySample>> allMercurySamples) {
        Map<String, MercurySample.MetadataSource> results = new HashMap<>();

        for (String sampleName : sampleNames) {
            Multiset<MercurySample.MetadataSource> metadataSources = HashMultiset.create();

            List<MercurySample> mercurySamples = allMercurySamples.get(sampleName);
            MercurySample.MetadataSource metadataSource;
            for (MercurySample mercurySample : mercurySamples) {
                metadataSources.add(mercurySample.getMetadataSource());
            }

            if (metadataSources.isEmpty()) {
                metadataSource = MercurySample.MetadataSource.BSP;
            } else if (metadataSources.elementSet().size() > 1) {
                String metadataSourceCounts = "";
                for (MercurySample.MetadataSource source : metadataSources) {
                    metadataSourceCounts += String.format("%s: %d ", source, metadataSources.count(source));
                }
                throw new RuntimeException(
                        String.format("There are MercurySamples for %s that disagree on the sample data source: %s",
                                sampleName, metadataSourceCounts));
            } else {
                metadataSource = metadataSources.iterator().next();
            }
            results.put(sampleName, metadataSource);
        }
        return results;
    }
}
