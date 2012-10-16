package org.broadinstitute.gpinformatics.mercury.entity.sample;
// todo jmt this should be in the control.dao package

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPStartingSample_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Stateful
@RequestScoped
public class BSPStartingSampleDAO extends GenericDao {

    public BSPStartingSample findBySampleName(String stockName) {
        return findSingle(BSPStartingSample.class, BSPStartingSample_.sampleName, stockName);
    }

    public Map<String, BSPStartingSample> findByNames(List<String> labels) {
        Map<String, BSPStartingSample> mapNameToSample = new LinkedHashMap<String, BSPStartingSample>();
        for (String label : labels) {
            mapNameToSample.put(label, null);
        }
        List<BSPStartingSample> bspStartingSamples =
                findListByList(BSPStartingSample.class, BSPStartingSample_.sampleName, labels);

        for (BSPStartingSample bspStartingSample : bspStartingSamples) {
            mapNameToSample.put(bspStartingSample.getSampleName(), bspStartingSample);
        }

        return mapNameToSample;
    }
}
