package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

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

    public SampleDataFetcher(@Nonnull BSPSampleDataFetcher bspSampleDataFetcher, MercurySampleDao mercurySampleDao) {
        this.bspSampleDataFetcher = bspSampleDataFetcher;
        this.mercurySampleDao = mercurySampleDao;
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
    public BSPSampleDTO fetchSampleData(String sampleName) {
        BSPSampleDTO bspSampleDTO = null;
        if (BSPUtil.isInBspFormat(sampleName)) {
            bspSampleDTO = bspSampleDataFetcher.fetchSingleSampleFromBSP(sampleName);
        }
        return bspSampleDTO;
    }

    /**
     * Fetch the data from bsp for multiple samples.
     *
     * @param sampleNames The sample names, which can be short barcodes such as SM-4FHTK,
     *                    or bare ids such as 4FHTK.
     *
     * @return Mapping of sample id to its bsp data
     */
    public Map<String, BSPSampleDTO> fetchSampleData(@Nonnull Collection<String> sampleNames) {
        List<String> bspSamples = new ArrayList<>();
        for (String sampleName : sampleNames) {
            if (BSPUtil.isInBspFormat(sampleName)) {
                bspSamples.add(sampleName);
            }
        }
        Map<String, BSPSampleDTO> sampleData = Collections.emptyMap();
        if (!bspSamples.isEmpty()) {
            sampleData = bspSampleDataFetcher.fetchSamplesFromBSP(bspSamples);
        }
        return sampleData;
    }

    /**
     * There is much copying and pasting of code from BSPSampleSearchServiceImpl into here -- a refactoring is needed.
     *
     * @param bspSampleDTOs BSP DTOs whose sampleID field will be referenced for the barcode value, and which will
     *                      be filled with the ffpeDerived value returned by the FFPE webservice
     */
    public void fetchFFPEDerived(@Nonnull Collection<BSPSampleDTO> bspSampleDTOs) {

        // Check to see if BSP is supported before trying to get data.
        bspSampleDataFetcher.fetchFFPEDerived(bspSampleDTOs);
    }

    public void fetchSamplePlastic(@Nonnull Collection<BSPSampleDTO> bspSampleDTOs) {

        bspSampleDataFetcher.fetchSamplePlastic(bspSampleDTOs);
    }

    /**
     * Given an aliquot ID, return its stock sample ID.
     */
    public String getStockIdForAliquotId(@Nonnull String aliquotId) {
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKey(aliquotId);
        Multiset<MercurySample.MetadataSource> metadataSources = HashMultiset.create();
        for (MercurySample mercurySample : mercurySamples) {
            metadataSources.add(mercurySample.getMetadataSource());
        }
        MercurySample.MetadataSource metadataSource;
        if (metadataSources.isEmpty()) {
            metadataSource = MercurySample.MetadataSource.BSP;
        } else if (metadataSources.size() > 1) {
            String metadataSourceCounts = "";
            for (MercurySample.MetadataSource source : metadataSources) {
                metadataSourceCounts += String.format("%s: %d", source, metadataSources.count(source));
            }
            throw new RuntimeException(
                    String.format("There are MercurySamples for %s that disagree on the sample data source: %s",
                            aliquotId, metadataSourceCounts));
        } else {
            metadataSource = metadataSources.iterator().next();
        }

        switch (metadataSource) {
        case BSP:
            return bspSampleDataFetcher.getStockIdForAliquotId(aliquotId);
        case MERCURY:
            return aliquotId;
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
        for (MercurySample mercurySample : mercurySamples) {
            sampleIdsByMetadataSource.put(mercurySample.getMetadataSource(), mercurySample.getSampleKey());
        }

        Map<String, String> stockIdByAliquotId = new HashMap<>();

        // Handle BSP samples.
        Collection<String> bspSampleIds = sampleIdsByMetadataSource.get(MercurySample.MetadataSource.BSP);
        stockIdByAliquotId.putAll(bspSampleDataFetcher.getStockIdByAliquotId(bspSampleIds));

        // Handle Mercury samples.
        for (String sampleId : sampleIdsByMetadataSource.get(MercurySample.MetadataSource.MERCURY)) {
            stockIdByAliquotId.put(sampleId, sampleId);
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
}
