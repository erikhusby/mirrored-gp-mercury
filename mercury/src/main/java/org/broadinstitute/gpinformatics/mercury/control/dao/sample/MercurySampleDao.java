package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for MercurySample
 */
@Stateful
@RequestScoped
public class MercurySampleDao extends GenericDao {

    /**
     * Finds MercurySamples that correspond to a list.
     *
     * @param mercurySamples list of keys to search for.  The entity is currently its own key.
     * @return map from input to found, or null if not found, in same order as input list
     */
    public Map<MercurySample, MercurySample> findByMercurySample(List<MercurySample> mercurySamples) {
        HashMap<MercurySample, MercurySample> mapSampleToSample = new HashMap<>();

        List<String> sampleKeys = new ArrayList<>();
        for (MercurySample mercurySample : mercurySamples) {
            sampleKeys.add(mercurySample.getSampleKey());
            mapSampleToSample.put(mercurySample, null);
        }

        List<MercurySample> resultList = findListByList(MercurySample.class, MercurySample_.sampleKey, sampleKeys);
        for (MercurySample mercurySample : resultList) {
            // We're fetching by sample key only, so we could get samples in multiple product orders, hence
            // check that the fetched entity matches one of the inputs
            if (mapSampleToSample.containsKey(mercurySample)) {
                mapSampleToSample.put(mercurySample, mercurySample);
            }
        }

        return mapSampleToSample;
    }

    /**
     * Find MercurySamples by their sample keys.
     *
     * @param sampleKeys list of sample key strings to search for.
     * @return list of mercury samples from sample key lookup
     */
    public List<MercurySample> findBySampleKeys(@Nonnull Collection<String> sampleKeys) {
        return findListByList(MercurySample.class, MercurySample_.sampleKey, sampleKeys);
    }

    public MercurySample findBySampleKey(String sampleKey) {
        return findSingle(MercurySample.class, MercurySample_.sampleKey, sampleKey);
    }

    /**
     * Finds MercurySamples for the given samples keys. Returns a map of MercurySamples indexed by sample key. The map
     * contains empty collections if no MercurySamples are found.
     *
     * @param sampleKeys    the sample keys to search for
     * @return a map of sample key to (possibly multiple) MercurySamples
     */
    public Map<String, List<MercurySample>> findMapIdToListMercurySample(Collection<String> sampleKeys) {
        Map<String, List<MercurySample>> mapSampleIdToListMercurySamples = new HashMap<>();
        for (String sampleKey : sampleKeys) {
            mapSampleIdToListMercurySamples.put(sampleKey, new ArrayList<MercurySample>());
        }
        List<MercurySample> mercurySamples = findListByList(MercurySample.class, MercurySample_.sampleKey, sampleKeys);
        for (MercurySample mercurySample : mercurySamples) {
            mapSampleIdToListMercurySamples.get(mercurySample.getSampleKey()).add(mercurySample);
        }
        return mapSampleIdToListMercurySamples;
    }

    /**
     * Finds MercurySamples for the given samples keys. Returns a map of MercurySamples indexed by sample key. The map
     * contains empty collections if no MercurySamples are found.
     *
     * @param sampleKeys    the sample keys to search for
     * @return a map of sample key to (possibly multiple) MercurySamples
     */
    public Map<String, MercurySample> findMapIdToMercurySample(Collection<String> sampleKeys) {
        Map<String, MercurySample> mapSampleIdToListMercurySamples = new HashMap<>();
        List<MercurySample> mercurySamples = findListByList(MercurySample.class, MercurySample_.sampleKey, sampleKeys);
        for (MercurySample mercurySample : mercurySamples) {
            mapSampleIdToListMercurySamples.put(mercurySample.getSampleKey(), mercurySample);
        }
        return mapSampleIdToListMercurySamples;
    }

}
