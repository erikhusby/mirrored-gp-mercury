package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
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
        List<LabVessel> resultList = new ArrayList<LabVessel>();

        for(String currSample:sampleKeys) {
            resultList.addAll(findBySampleKey(currSample));
        }

        return resultList;
    }

    public List<LabVessel> findBySampleKey(String sampleKey) {
        List<LabVessel> resultList = new ArrayList<LabVessel>();
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
        List<LabVessel> resultList = new ArrayList<LabVessel>();

        for(String currPdoKey: productOrderKeys) {
            resultList.addAll(findByPDOKey(currPdoKey));
        }

        return resultList;
    }

    public List<LabVessel> findByPDOKey(String productOrderKey) {
        List<LabVessel> resultList = new ArrayList<LabVessel>();
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
        Map<String, LabVessel> mapBarcodeToTube = new TreeMap<String, LabVessel>();
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
