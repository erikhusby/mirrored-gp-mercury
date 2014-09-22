package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class MercurySampleDataFetcher {
    public Map<String, MercurySampleData> fetchSampleData(Collection<MercurySample> mercurySamples) {
        Map<String, MercurySampleData> results = new HashMap<>();
        for (MercurySample mercurySample : mercurySamples) {
            results.put(mercurySample.getSampleKey(),
                    new MercurySampleData(mercurySample.getSampleKey(), mercurySample.getMetadata()));
        }
        return results;
    }

    public MercurySampleData fetchSampleData(MercurySample sample) {
        return fetchSampleData(Collections.singleton(sample)).get(sample.getSampleKey());
    }

    public String getStockIdForAliquotId(String aliquotId) {
        return null;
//        return aliquotId; // TODO: use this instead after adding a test in MercurySampleDataFetcherTest
    }

    public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {
        return null;
    }
}
