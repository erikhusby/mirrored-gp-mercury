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
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Stateful
@RequestScoped
public class LabVesselDao extends GenericDao {

    public LabVessel findByIdentifier(String barcode) {
        return findSingle(LabVessel.class, LabVessel_.label, barcode);
    }

    public List<LabVessel> findByListIdentifiers(List<String> barcodes) {
        return findListByList(LabVessel.class, LabVessel_.label, barcodes);
    }

    public List<LabVessel> findBySampleKeyList(List<String> sampleKeys) {
        return findBySampleKeyList((Collection<String>) sampleKeys);
    }

    public List<LabVessel> findBySampleKeyList(Collection<String> sampleKeys) {
        List<LabVessel> resultList = new ArrayList<>();

        for (String sampleKey : sampleKeys) {
            resultList.addAll(findBySampleKey(sampleKey));
        }

        return resultList;
    }

    /**
     * This is used to create a map containing a list of LabVessel objects keyed off the associated sample key.
     *
     * @param sampleKeys The list of sample keys to search for associated LabVessel objects.
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
                            Collection<String> sampleKeys = new ArrayList<> ();
                            for (ProductOrderSample sample : parameterList) {
                                sampleKeys.add(sample.getBusinessKey());
                            }
                            criteriaQuery.where(labVessels.get(MercurySample_.sampleKey).in(sampleKeys));
                            return getEntityManager().createQuery(criteriaQuery);
                        }
                    });
            } catch (NoResultException ignored) {
                resultList = new ArrayList<> ();
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
}
