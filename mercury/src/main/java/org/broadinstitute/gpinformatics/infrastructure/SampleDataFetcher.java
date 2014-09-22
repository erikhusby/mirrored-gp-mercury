package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link BSPSampleSearchService} that
 * does a bit more object-ifying and type-safety.
 */
public class SampleDataFetcher {

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
     * Fetch the data from BSP for the given sample.
     *
     * @param sampleName The sample name.
     *
     * @return The sample DTO that was fetched.
     */
    public SampleData fetchSampleData(String sampleName) {
        return fetchSampleData(Collections.singleton(sampleName)).get(sampleName);
    }

    /**
     * Fetch the data from bsp for multiple samples.
     *
     * @param sampleNames The sample names, which can be short barcodes such as SM-4FHTK,
     *                    or bare ids such as 4FHTK.
     *
     * @return Mapping of sample id to its bsp data
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
                if (BSPUtil.isInBspFormat(sampleName)) {
                    bspSampleIds.add(sampleName);
                }
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
            Map<String, BSPSampleDTO> bspSampleData = bspSampleDataFetcher.fetchSamplesFromBSP(bspSampleIds);
            sampleData.putAll(bspSampleData);
        }
        sampleData.putAll(mercurySampleDataFetcher.fetchSampleData(mercurySamples));
        return sampleData;
    }

    /**
     * There is much copying and pasting of code from BSPSampleSearchServiceImpl into here -- a refactoring is needed.
     *
     * @param sampleDatas BSP DTOs whose sampleID field will be referenced for the barcode value, and which will
     *                    be filled with the ffpeDerived value returned by the FFPE webservice
     */
    public void fetchFFPEDerived(@Nonnull Collection<SampleData> sampleDatas) {

        // Check to see if BSP is supported before trying to get data.
        bspSampleDataFetcher.fetchFFPEDerived(BSPSampleDataFetcher.convertToBSPSampleDTOCollection(sampleDatas));
    }

    public void fetchSamplePlastic(@Nonnull Collection<SampleData> sampleDatas) {

        bspSampleDataFetcher.fetchSamplePlastic(BSPSampleDataFetcher.convertToBSPSampleDTOCollection(sampleDatas));
    }

    /**
     * Given an aliquot ID, return its stock sample ID.
     */
    public String getStockIdForAliquotId(@Nonnull String aliquotId) {
        Collection<String> sampleNames = Collections.singleton(aliquotId);
        Map<String, List<MercurySample>> allMercurySamples = mercurySampleDao.findMapIdToListMercurySample(sampleNames);
        MercurySample.MetadataSource metadataSource =
                determineMetadataSource(sampleNames, allMercurySamples).get(aliquotId);

        switch (metadataSource) {
        case BSP:
            return bspSampleDataFetcher.getStockIdForAliquotId(aliquotId);
        case MERCURY:
            // Even if there is > 1 MercurySample in the result of findMapIdToList the sample id will still be the same.
            return mercurySampleDataFetcher.getStockIdForAliquotId(allMercurySamples.get(aliquotId).get(0));
        default:
            throw new IllegalStateException("Unknown sample data source: " + metadataSource);
        }
    }

    /**
     * Given a list of aliquot IDs, return a map of aliquot IDs to stock IDs.
     */
    public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(aliquotIds);
        Multimap<MercurySample.MetadataSource, String> sampleIdsByMetadataSource = ArrayListMultimap.create();
        Collection<MercurySample> mercuryOnlySamples=new ArrayList<>();
        for (MercurySample mercurySample : mercurySamples) {
            sampleIdsByMetadataSource.put(mercurySample.getMetadataSource(), mercurySample.getSampleKey());
            if (mercurySample.getMetadataSource() == MercurySample.MetadataSource.MERCURY) {
                mercuryOnlySamples.add(mercurySample);
            }
        }

        // Assume that BSP is the owner of samples data for sample IDs with no MercurySample.
        Collection<String> unknownBspSamples = CollectionUtils.subtract(aliquotIds, sampleIdsByMetadataSource.values());
        sampleIdsByMetadataSource.putAll(MercurySample.MetadataSource.BSP, unknownBspSamples);

        Map<String, String> stockIdByAliquotId = new HashMap<>();

        // Handle BSP samples.
        if (sampleIdsByMetadataSource.containsKey(MercurySample.MetadataSource.BSP)) {
            Collection<String> bspSampleIds = sampleIdsByMetadataSource.get(MercurySample.MetadataSource.BSP);
            stockIdByAliquotId.putAll(bspSampleDataFetcher.getStockIdByAliquotId(bspSampleIds));
        }
        // Handle Mercury samples.
        if (! mercuryOnlySamples.isEmpty()) {
            stockIdByAliquotId.putAll(mercurySampleDataFetcher.getStockIdByAliquotId(mercuryOnlySamples));
        }

        return stockIdByAliquotId;
    }

    /**
     * Returns a map of barcode to SampleDetails object for each input barcode, which can be a manufacturer barcode
     * or SM-id barcode.
     */
    public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByBarcode(@Nonnull Collection<String> barcodes) {

        // Use POST, rather than GET, to allow large number of barcodes without hitting 8K limit on URL.

        // Fills in the map values using SampleDetails that were found in BSP.

        return bspSampleDataFetcher.fetchSampleDetailsByBarcode(barcodes);
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
            } else if (metadataSources.size() > 1) {
                String metadataSourceCounts = "";
                for (MercurySample.MetadataSource source : metadataSources) {
                    metadataSourceCounts += String.format("%s: %d", source, metadataSources.count(source));
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
