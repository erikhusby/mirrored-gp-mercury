package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LabVesselDao extends GenericDao {

    public LabVessel findByIdentifier(String barcode) {
        return findSingle(LabVessel.class, LabVessel_.label, barcode);
    }

    public List<LabVessel> findByListIdentifiers(List<String> barcodes) {
        return findListByList(LabVessel.class, LabVessel_.label, barcodes);
    }

    public List<LabVessel> findByTickets(String ticket) {

        List<LabVessel> resultList = new ArrayList<>();
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LabVessel> criteriaQuery = criteriaBuilder.createQuery(LabVessel.class);
        Root<LabVessel> root = criteriaQuery.from(LabVessel.class);
        Join<LabVessel, JiraTicket> labVessels = root.join(LabVessel_.ticketsCreated);
        Predicate predicate = criteriaBuilder.equal(labVessels.get(JiraTicket_.ticketId), ticket);
        criteriaQuery.where(predicate);
        try {
            resultList.addAll(getEntityManager().createQuery(criteriaQuery).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }
        return resultList;
    }

    public LabVessel findByListIdentifiers(Long vid) {
        return findSingle(LabVessel.class, LabVessel_.labVesselId, vid);
    }

    public List<LabVessel> findBySampleKeyOrLabVesselLabel(Collection<String> sampleKeys) {

        LinkedHashSet<LabVessel> labVessels = new LinkedHashSet<>();
        List<MercurySample> mercurySamples = findListByList(MercurySample.class, MercurySample_.sampleKey, sampleKeys);

        for (MercurySample mercurySample : mercurySamples) {
            labVessels.addAll(mercurySample.getLabVessel());
        }

        labVessels.addAll(findListByList(LabVessel.class, LabVessel_.label, sampleKeys));

        return new ArrayList<>(labVessels);
    }

    /**
     * This is used to create a map containing a list of LabVessel objects keyed off the associated sample key.
     *
     * @param samples The list of product order sample to search for associated LabVessel objects.
     *
     * @return A map containing a list of LabVessel objects keyed off the associated sample key.
     */
    public Multimap<String, LabVessel> findMapBySampleKeys(Collection<ProductOrderSample> samples) {
        Multimap<String, LabVessel> resultMap = HashMultimap.create();

        if (!CollectionUtils.isEmpty(samples)) {

            CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
            final CriteriaQuery<LabVessel> criteriaQuery = criteriaBuilder.createQuery(LabVessel.class);
            final Root<LabVessel> root = criteriaQuery.from(LabVessel.class);
            final Join<LabVessel, MercurySample> labVessels = root.join(LabVessel_.mercurySamples);

            List<LabVessel> resultList;
            try {
                resultList = JPASplitter.runCriteriaQuery(
                    samples,
                    new CriteriaInClauseCreator<ProductOrderSample>() {
                        @Override
                        public Query createCriteriaInQuery(Collection<ProductOrderSample> parameterList) {
                            Collection<String> sampleKeys = new ArrayList<>();
                            for (ProductOrderSample sample : parameterList) {
                                sampleKeys.add(sample.getBusinessKey());
                            }
                            criteriaQuery.where(labVessels.get(MercurySample_.sampleKey).in(sampleKeys));
                            return getEntityManager().createQuery(criteriaQuery);
                        }
                    });
            } catch (NoResultException ignored) {
                return resultMap;
            }

            // For each LabVessel found, add it to the list of LabVessel objects for the applicable sample in the map.
            for (LabVessel result : resultList) {
                // A LabVessel can contain multiple samples (pooled samples) so we must loop through them.
                for (MercurySample sample : result.getMercurySamples()) {
                    resultMap.put(sample.getSampleKey(), result);
                }
            }
        }

        return resultMap;
    }

    public List<LabVessel> findBySampleKey(String sampleKey) {
        List<LabVessel> resultList = new ArrayList<>();
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LabVessel> criteriaQuery = criteriaBuilder.createQuery(LabVessel.class);
        Root<LabVessel> root = criteriaQuery.from(LabVessel.class);
        Join<LabVessel, MercurySample> labVessels = root.join(LabVessel_.mercurySamples);
        Predicate predicate = criteriaBuilder.equal(labVessels.get(MercurySample_.sampleKey), sampleKey);
        criteriaQuery.where(predicate);
        try {
            resultList.addAll(getEntityManager().createQuery(criteriaQuery).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }
        return resultList;
    }

    public List<LabVessel> findByPDOKeyList(List<String> productOrderKeys) {
        List<LabVessel> resultList = new ArrayList<>();

        for (String currPdoKey : productOrderKeys) {
            resultList.addAll(findByPDOKey(currPdoKey));
        }

        return resultList;
    }

    public List<LabVessel> findByPDOKey(String productOrderKey) {
        List<LabVessel> resultList = new ArrayList<>();
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<LabVessel> criteriaQuery = criteriaBuilder.createQuery(LabVessel.class);
        Root<LabVessel> root = criteriaQuery.from(LabVessel.class);
        Join<LabVessel, BucketEntry> bucketEntryJoin = root.join(LabVessel_.bucketEntries);
        Predicate predicate = criteriaBuilder.equal(bucketEntryJoin.get(BucketEntry_.poBusinessKey), productOrderKey);
        criteriaQuery.where(predicate);
        try {
            resultList.addAll(getEntityManager().createQuery(criteriaQuery).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }
        return resultList;
    }

    public Map<String, LabVessel> findByBarcodes(List<String> barcodes) {
        Map<String, LabVessel> mapBarcodeToTube = new TreeMap<>();
        for (String barcode : barcodes) {
            mapBarcodeToTube.put(barcode, null);
        }
        List<LabVessel> results = findListByList(LabVessel.class, LabVessel_.label, barcodes);
        for (LabVessel result : results) {
            mapBarcodeToTube.put(result.getLabel(), result);
        }
        return mapBarcodeToTube;
    }

    public List<LabVessel> findAllWithEventButMissingAnother(final List<LabEventType> searchEventTypes,
                                                             final LabEventType missingEventType) {
        List<LabVessel> resultList = new ArrayList<>();

        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<LabVessel> existsQuery = builder.createQuery(LabVessel.class);
        Root<LabVessel> existsRoot = existsQuery.from(LabVessel.class);
        Join<LabVessel, LabEvent> labVessels = existsRoot.join(LabVessel_.inPlaceLabEvents);

        // Build sub query for all labvessels that have the 'missing' event to filter them out in the main query
        Subquery<Long> missingQuery = existsQuery.subquery(Long.class);
        Root<LabVessel> missingRoot = missingQuery.from(LabVessel.class);
        Join<LabVessel, LabEvent> missingVesselJoin = missingRoot.join(LabVessel_.inPlaceLabEvents);
        missingQuery.select(missingRoot.get(LabVessel_.labVesselId)).
                where(builder.equal(missingVesselJoin.get(LabEvent_.labEventType), missingEventType));

        // Put them together
        existsQuery.select(existsRoot).where(labVessels.get(LabEvent_.labEventType).in(searchEventTypes),
                        builder.in(existsRoot.get(LabVessel_.labVesselId)).value(missingQuery).not());

        try {
            resultList.addAll(getEntityManager().createQuery(existsQuery).getResultList());
        } catch (NoResultException ignored) {
            return resultList;
        }

        return resultList;
    }
}
