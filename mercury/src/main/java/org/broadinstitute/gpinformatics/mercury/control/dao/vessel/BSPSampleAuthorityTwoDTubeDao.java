package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BSPSampleAuthorityTwoDTube_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for BSPSampleAuthorityTwoDTube
 */
@Stateful
@RequestScoped
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

        List<BSPSampleAuthorityTwoDTube> bspSampleAuthorityTwoDTubes =
                findListByList(BSPSampleAuthorityTwoDTube.class, BSPSampleAuthorityTwoDTube_.label, labels);

        for (BSPSampleAuthorityTwoDTube bspSampleAuthorityTwoDTube : bspSampleAuthorityTwoDTubes) {
            mapLabelToTube.put(bspSampleAuthorityTwoDTube.getLabel(), bspSampleAuthorityTwoDTube);
        }

        return mapLabelToTube;
    }
}
