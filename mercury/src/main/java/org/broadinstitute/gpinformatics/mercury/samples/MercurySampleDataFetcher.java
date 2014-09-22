package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mercury specific version of SampleDataFetcher.
 */
public class MercurySampleDataFetcher {
    /**
     * Fetch the sample data for a collection of MercurySamples.
     *
     * @return a map of MercurySampleData keyed on MercurySample's sampleKey()
     */
    public Map<String, MercurySampleData> fetchSampleData(@Nonnull Collection<MercurySample> mercurySamples) {
        Map<String, MercurySampleData> results = new HashMap<>();
        for (MercurySample mercurySample : mercurySamples) {
            results.put(mercurySample.getSampleKey(),
                    new MercurySampleData(mercurySample.getSampleKey(), mercurySample.getMetadata()));
        }
        return results;
    }

    /**
     * Fetch the sample data for one MercurySample.
     *
     * @return MercurytSampleData for the input sample.
     */
    public MercurySampleData fetchSampleData(@Nonnull MercurySample sample) {
        return fetchSampleData(Collections.singleton(sample)).get(sample.getSampleKey());
    }

    /**
     * Find the StockID for a MercurySample
     *
     * @param mercurySample Sample to get the stockId from;
     *
     * @return stockId for the MercurySample, which is the sampleKey()
     */
    public String getStockIdForAliquotId(@Nonnull MercurySample mercurySample) {
        return mercurySample.getSampleKey();
    }

    /**
     * Find the StockID for a collection of MercurySamples
     *
     * @param mercurySamples Sample to get the stockId from;
     *
     * @return a map from sampleId to stockId
     */
    public Map<String, String> getStockIdByAliquotId(@Nonnull Collection<MercurySample> mercurySamples) {
        Map<String, String> results = new HashMap<>();
        for (MercurySample mercurySample : mercurySamples) {
            results.put(mercurySample.getSampleKey(), getStockIdForAliquotId(mercurySample));
        }
        return results;
    }
}
