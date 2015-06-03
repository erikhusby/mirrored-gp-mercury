package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
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
     * THIS METHOD IS DEFUNCT.  Only still here because it is called in a fixup test and we cannot modify fixup tests
     * after they are persisted
     *
     * The Proper Method to use is:
     *
     * @see #findMapIdToMercurySample
     *
     *
     * @param sampleKeys    the sample keys to search for
     * @return a map of sample key to (possibly multiple) MercurySamples
     */
    @Deprecated
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
     * contains entries only for MercurySamples that are found.
     *
     * @param sampleKeys    the sample keys to search for
     * @return a map of sample key to a MercurySample
     */
    public Map<String, MercurySample> findMapIdToMercurySample(Collection<String> sampleKeys) {
        Map<String, MercurySample> mapSampleIdToListMercurySamples = new HashMap<>();
        List<MercurySample> mercurySamples = findListByList(MercurySample.class, MercurySample_.sampleKey, sampleKeys);
        for (MercurySample mercurySample : mercurySamples) {
            mapSampleIdToListMercurySamples.put(mercurySample.getSampleKey(), mercurySample);
        }
        return mapSampleIdToListMercurySamples;
    }

    /**
     * This method was only created for the purpose of a fixup test that would find and eliminate Duplicate entries
     * for MercurySamples.  It will never be needed again since we took steps to prevent any instances of duplicate
     * MercurySamples within the Mercury environment.
     * @return
     */
    public List<MercurySample> findDuplicateSamples() {

        List<MercurySample> results = new ArrayList<>();

        CriteriaBuilder sampleBuilder = getEntityManager().getCriteriaBuilder();

        ParameterExpression<Long> minimumCount = sampleBuilder.parameter(Long.class);
        CriteriaQuery<MercurySample> duplicateSampleQuery = sampleBuilder.createQuery(MercurySample.class);

        Subquery<String> dupeSampleNameQuery = duplicateSampleQuery.subquery(String.class);
        Root<MercurySample> sampleNameQ_Root = dupeSampleNameQuery.from(MercurySample.class);

        dupeSampleNameQuery.select(sampleNameQ_Root.get(MercurySample_.sampleKey))
                .distinct(true)
                .groupBy(sampleNameQ_Root.get(MercurySample_.sampleKey))
                .having(sampleBuilder.gt(sampleBuilder.count(sampleNameQ_Root), minimumCount));

        Root<MercurySample> sampleRoot = duplicateSampleQuery.from(MercurySample.class);

        duplicateSampleQuery.select(sampleRoot).distinct(true)
                .where(sampleBuilder.in(sampleRoot.get(MercurySample_.sampleKey)).value(dupeSampleNameQuery));

        try {
            TypedQuery<MercurySample> query = getEntityManager().createQuery(duplicateSampleQuery);
            query.setParameter(minimumCount, 1L);
            results.addAll(query.getResultList());
        } catch (NoResultException ignored) {
        }
        return results;
    }

    /**
     * This method was written for a fixup test. It finds all samples with MetadataSource which do not have
     * the specified metadataKeys.
     */
    public List<MercurySample> findSamplesWithoutMetadata(@Nonnull MercurySample.MetadataSource metadataSource,
                                                          @Nonnull Metadata.Key... metadataKeys) {
        CriteriaBuilder sampleBuilder = getEntityManager().getCriteriaBuilder();
        ParameterExpression<MercurySample.MetadataSource> metadataSourceParameter = sampleBuilder.parameter(
                MercurySample.MetadataSource.class);

        CriteriaQuery<MercurySample> samplesWithoutMetadataKeysQuery = sampleBuilder.createQuery(MercurySample.class);

        Subquery<String> samplesWithoutMetadataKeysSubQuery = samplesWithoutMetadataKeysQuery.subquery(String.class);
        Root<MercurySample> samplesWithoutKeysRoot = samplesWithoutMetadataKeysSubQuery.from(MercurySample.class);
        Join<MercurySample, Metadata> metadataJoin = samplesWithoutKeysRoot.join(MercurySample_.metadata);
        samplesWithoutMetadataKeysSubQuery.select(samplesWithoutKeysRoot.get(MercurySample_.sampleKey))
                .distinct(true)
                .where(metadataJoin.get(Metadata_.key).in(metadataKeys));

        Root<MercurySample> sampleRoot = samplesWithoutMetadataKeysQuery.from(MercurySample.class);
        samplesWithoutMetadataKeysQuery.select(sampleRoot).distinct(true)
                .where(sampleBuilder.equal(sampleRoot.get(MercurySample_.metadataSource), metadataSourceParameter),
                        sampleBuilder.in(sampleRoot.get(MercurySample_.sampleKey))
                                .value(samplesWithoutMetadataKeysSubQuery).not());

        TypedQuery<MercurySample> query = getEntityManager().createQuery(samplesWithoutMetadataKeysQuery);
        query.setParameter(metadataSourceParameter, metadataSource);
        return query.getResultList();


    }
}
