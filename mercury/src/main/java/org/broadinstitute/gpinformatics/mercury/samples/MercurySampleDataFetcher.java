package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class MercurySampleDataFetcher {
    private MercurySampleDao mercurySampleDao;

    @Inject
    public MercurySampleDataFetcher(MercurySampleDao mercurySampleDao) {
        this.mercurySampleDao = mercurySampleDao;
    }

    public MercurySampleDataFetcher() {
    }

    public Map<String, MercurySampleData> fetchSampleData(Collection<String> sampleIds) {
        Map<String, MercurySampleData> results=new HashMap<>();
        for (MercurySample mercurySample : mercurySampleDao.findBySampleKeys(sampleIds)) {
            results.put(mercurySample.getSampleKey(), fetchSampleData(mercurySample.getSampleKey()));
        }
        return results;
    }

    public MercurySampleData fetchSampleData(String sampleName) {
        return null;
    }
}
