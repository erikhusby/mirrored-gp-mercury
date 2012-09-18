package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for BSPSampleAuthorityTwoDTube
 */
public class BSPSampleAuthorityTwoDTubeDao extends GenericDao {

    /**
     * Fetches tubes for a given list of labels.
     * @param labels list of labels
     * @return map from label to tube (may be null), iterable in same order as input list
     */
    public Map<String, BSPSampleAuthorityTwoDTube> findByLabels(List<String> labels) {
        Map<String, BSPSampleAuthorityTwoDTube> mapLabelToTube = new LinkedHashMap<String, BSPSampleAuthorityTwoDTube>();
        for (String label : labels) {
            mapLabelToTube.put(label, null);
        }

        EntityManager entityManager = this.getThreadEntityManager().getEntityManager();
        CriteriaQuery<BSPSampleAuthorityTwoDTube> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(BSPSampleAuthorityTwoDTube.class);
        Root<BSPSampleAuthorityTwoDTube> root = criteriaQuery.from(BSPSampleAuthorityTwoDTube.class);
        criteriaQuery.where(root.get(LabVessel_.label).in(labels));
        List<BSPSampleAuthorityTwoDTube> bspSampleAuthorityTwoDTubes =
                entityManager.createQuery(criteriaQuery).getResultList();

        for (BSPSampleAuthorityTwoDTube bspSampleAuthorityTwoDTube : bspSampleAuthorityTwoDTubes) {
            mapLabelToTube.put(bspSampleAuthorityTwoDTube.getLabel(), bspSampleAuthorityTwoDTube);
        }

        return mapLabelToTube;
    }
}
