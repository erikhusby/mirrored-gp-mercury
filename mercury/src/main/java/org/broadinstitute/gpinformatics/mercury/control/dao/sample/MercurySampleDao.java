package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
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
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
        SetJoin<MercurySample, LabVessel> sampleLabVesselSetJoin = sampleRoot.join(MercurySample_.labVessel);
        SetJoin<LabVessel, LabEvent> joinLabVesselEvents = sampleLabVesselSetJoin.join(LabVessel_.inPlaceLabEvents);

        samplesWithoutMetadataKeysQuery.select(sampleRoot).distinct(true)
                .where(sampleBuilder.equal(sampleRoot.get(MercurySample_.metadataSource), metadataSourceParameter),
                        sampleBuilder.in(sampleRoot.get(MercurySample_.sampleKey))
                                .value(samplesWithoutMetadataKeysSubQuery).not(), sampleBuilder
                                .equal(joinLabVesselEvents.get(LabEvent_.labEventType),
                                        LabEventType.COLLABORATOR_TRANSFER));

        TypedQuery<MercurySample> query = getEntityManager().createQuery(samplesWithoutMetadataKeysQuery);
        query.setParameter(metadataSourceParameter, metadataSource);
        return query.getResultList();


    }

    public Map<String, MercurySample> findNonReceivedCrspSamples() {
            /*
            select distinct samp.SAMPLE_KEY, tube.LABEL, meta.KEY, meta.VALUE, evt.EVENT_DATE,
            evt.LAB_EVENT_TYPE
            from mercury_sample samp
            join MERCURY_SAMPLE_METADATA sampmeta on sampmeta.MERCURY_SAMPLE = samp.MERCURY_SAMPLE_ID
            join metadata meta on meta.METADATA_ID = sampmeta.METADATA
            join LAB_VESSEL_MERCURY_SAMPLES tubesamp on tubesamp.MERCURY_SAMPLES = samp.MERCURY_SAMPLE_ID
            join lab_vessel tube on tube.LAB_VESSEL_ID = tubesamp.LAB_VESSEL
            join lab_event evt on evt.IN_PLACE_LAB_VESSEL = tube.LAB_VESSEL_ID
            and evt.LAB_EVENT_TYPE = 'COLLABORATOR_TRANSFER'
            and meta.METADATA_ID in
                (
                select metasub.METADATA_ID from metadata metasub
                   join MANIFEST_RECORD_METADATA recmeta on recmeta.METADATA_ID = metasub.METADATA_ID
                   join manifest_record rec on rec.MANIFEST_RECORD_ID = recmeta.MANIFEST_RECORD_ID
                   join manifest_session sess on sess.MANIFEST_SESSION_ID = rec.MANIFEST_SESSION_ID
                   join athena.research_project rp on rp.RESEARCH_PROJECT_ID = sess.RESEARCH_PROJECT_ID
                   where rp.JIRA_TICKET_KEY not in ('RP-876', 'RP-917', 'RP-805')
                )

            and samp.MERCURY_SAMPLE_ID not in
                (select samp.MERCURY_SAMPLE_ID from mercury_sample samp
                   join LAB_VESSEL_MERCURY_SAMPLES tubesamp on tubesamp.MERCURY_SAMPLES = samp.MERCURY_SAMPLE_ID
                   join lab_vessel tube on tube.LAB_VESSEL_ID = tubesamp.LAB_VESSEL
                   join lab_event evt1 on evt1.IN_PLACE_LAB_VESSEL = tube.LAB_VESSEL_ID
                   where samp.METADATA_SOURCE = 'MERCURY'
                   and evt1.LAB_EVENT_TYPE = 'SAMPLE_RECEIPT')
            */
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();

        CriteriaQuery<MercurySample> nonReceivedSampleQuery = builder.createQuery(MercurySample.class);

        Root<MercurySample> nonReceivedRoot = nonReceivedSampleQuery.from(MercurySample.class);
        Join<MercurySample,Metadata> metadataJoin = nonReceivedRoot.join(MercurySample_.metadata);
        Join<LabVessel, LabEvent> vesselJoin =
                nonReceivedRoot.join(MercurySample_.labVessel).join(LabVessel_.inPlaceLabEvents);



        // Build sub query for metadata that is derived from sessions which are not associated with Research
        // Projects that were used for: Controls or Dev Validation samples
        Subquery<Long> validMetadataSubQuery = nonReceivedSampleQuery.subquery(Long.class);
        Root<ManifestSession> includedSessionsRoot = validMetadataSubQuery.from(ManifestSession.class);
        Join<ManifestRecord, Metadata> recToMetadataJoin =
                includedSessionsRoot.join(ManifestSession_.records).join(ManifestRecord_.metadata);
        Join<ManifestSession, ResearchProject> researchProjectJoin =
                includedSessionsRoot.join(ManifestSession_.researchProject);

        validMetadataSubQuery.select(recToMetadataJoin.get(Metadata_.id)).distinct(true);


        // Build sub query to for all CRSP samples That HAVE been received.  This will be filtered out of the main
        // Query
        Subquery<String> receivedSampleQuery = nonReceivedSampleQuery.subquery(String.class);
        Root<MercurySample> receivedSamplesRoot = receivedSampleQuery.from(MercurySample.class);
        Join<LabVessel, LabEvent> receivedVesselJoin =
                receivedSamplesRoot.join(MercurySample_.labVessel).join(LabVessel_.inPlaceLabEvents);

        receivedSampleQuery.select(receivedSamplesRoot.get(MercurySample_.sampleKey)).distinct(true)
                .where(builder.equal(receivedSamplesRoot.get(MercurySample_.metadataSource),
                                MercurySample.MetadataSource.MERCURY),
                        builder.equal(receivedVesselJoin.get(LabEvent_.labEventType), LabEventType.SAMPLE_RECEIPT));

        // Put it all together in the big query shown in the comments above
        nonReceivedSampleQuery.select(nonReceivedRoot).distinct(true)
                .where(builder.equal(vesselJoin.get(LabEvent_.labEventType),LabEventType.COLLABORATOR_TRANSFER),
                        builder.in(metadataJoin.get(Metadata_.id)).value(validMetadataSubQuery),
                        builder.in(nonReceivedRoot.get(MercurySample_.sampleKey)).value(receivedSampleQuery).not());

        TypedQuery<MercurySample> query = getEntityManager().createQuery(nonReceivedSampleQuery);
        return Maps.uniqueIndex(query.getResultList(), new Function<MercurySample, String>() {
            @Override
            public String apply(MercurySample mercurySample) {
                return mercurySample.getSampleKey();
            }
        });
    }
}
