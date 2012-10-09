package org.broadinstitute.gpinformatics.mercury.entity.sample;
// todo jmt this should be in the control.dao package

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Stateful
@RequestScoped
public class BSPStartingSampleDAO extends GenericDao {

    public BSPStartingSample findBySampleName(String stockName) {
        BSPStartingSample bspStartingSample = null;
        try {
            bspStartingSample = (BSPStartingSample) this.getThreadEntityManager().getEntityManager().
                    createNamedQuery("BSPStartingSample.fetchBySampleName").
                    setParameter("sampleName", stockName).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return bspStartingSample;
    }

    public Map<String, BSPStartingSample> findByNames(List<String> labels) {
        Map<String, BSPStartingSample> mapNameToSample = new LinkedHashMap<String, BSPStartingSample>();
        for (String label : labels) {
            mapNameToSample.put(label, null);
        }

        EntityManager entityManager = this.getThreadEntityManager().getEntityManager();
        CriteriaQuery<BSPStartingSample> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(BSPStartingSample.class);
        Root<BSPStartingSample> root = criteriaQuery.from(BSPStartingSample.class);
        criteriaQuery.where(root.get(BSPStartingSample_.sampleName).in(labels));
        List<BSPStartingSample> bspStartingSamples = entityManager.createQuery(criteriaQuery).getResultList();

        for (BSPStartingSample bspStartingSample : bspStartingSamples) {
            mapNameToSample.put(bspStartingSample.getSampleName(), bspStartingSample);
        }

        return mapNameToSample;
    }
}
