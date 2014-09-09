package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
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
    public BSPSampleDTO fetchSingleSampleFromBSP(String sampleName) {
        return bspSampleDataFetcher.fetchSingleSampleFromBSP(sampleName);
    }

    /**
     * Fetch the data from bsp for multiple samples.
     *
     * @param sampleNames The sample names, which can be short barcodes such as SM-4FHTK,
     *                    or bare ids such as 4FHTK.
     * @param bspSampleSearchColumns Array of columns to return from BSP. The SAMPLE_ID column is always returned
     *                               no matter what values is provided for bspSampleSearchColumns as it is the key
     *                               to the map returned.
     *
     * @return Mapping of sample id to its bsp data
     */
    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames,
                                                         BSPSampleSearchColumn... bspSampleSearchColumns) {

        return bspSampleDataFetcher.fetchSamplesFromBSP(sampleNames, bspSampleSearchColumns);
    }

    /**
     * Fetch the data from bsp for multiple samples.
     *
     * @param sampleNames The sample names, which can be short barcodes such as SM-4FHTK,
     *                    or bare ids such as 4FHTK.
     *
     * @return Mapping of sample id to its bsp data
     */
    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames) {
        return bspSampleDataFetcher.fetchSamplesFromBSP(sampleNames);
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
        return bspSampleDataFetcher.getStockIdForAliquotId(aliquotId);
    }

    /**
     * Given a list of aliquot IDs, return a map of aliquot IDs to stock IDs.
     */
    public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {
        return bspSampleDataFetcher.getStockIdByAliquotId(aliquotIds);
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
