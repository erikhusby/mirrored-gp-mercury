package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mercury specific version of SampleDataFetcher.
 */
@Dependent
public class MercurySampleDataFetcher {
    /**
     * Fetch the sample data for a collection of MercurySamples.
     *
     * @return a map of MercurySampleData keyed on MercurySample's sampleKey()
     */
    public Map<String, MercurySampleData> fetchSampleData(@Nonnull Collection<MercurySample> mercurySamples) {
        Map<String, MercurySampleData> results = new HashMap<>();
        for (MercurySample mercurySample : mercurySamples) {
            results.put(mercurySample.getSampleKey(), new MercurySampleData(mercurySample));
        }
        return results;
    }

    /**
     * Fetch the sample data for one MercurySample.
     *
     * @return MercurySampleData for the input sample.
     */
    public MercurySampleData fetchSampleData(@Nonnull MercurySample sample) {
        return fetchSampleData(Collections.singleton(sample)).get(sample.getSampleKey());
    }

    /**
     * Find the StockID for a MercurySample.
     *
     * The usage of this method is to find the PDO sample given an aliquot sample ID from the pipeline. For workflows
     * where the PDO is created prior to plating and the samples are coming from BSP, finding the stock ID for the
     * aliquot ID is the right thing to do. For workflows where the PDO is created for samples that have already been
     * plated and the sample data is stored in Mercury, the aliquot and the "stock" are the same sample.
     *
     * @param mercurySample Sample to get the stockId from;
     *
     * @return stockId for the MercurySample, which is the sampleKey()
     */
    public String getStockIdForAliquotId(@Nonnull MercurySample mercurySample) {
        return fetchSampleData(mercurySample).getStockSample();
    }

    /**
     * Find the StockID for a collection of MercurySamples.
     *
     * @param mercurySamples Sample to get the stockId from;
     * @return a map from sampleId to stockId
     * @see #getStockIdForAliquotId(MercurySample)
     */
    public Map<String, String> getStockIdByAliquotId(@Nonnull Collection<MercurySample> mercurySamples) {
        Map<String, String> results = new HashMap<>();
        for (MercurySample mercurySample : mercurySamples) {
            results.put(mercurySample.getSampleKey(), getStockIdForAliquotId(mercurySample));
        }
        return results;
    }
}
