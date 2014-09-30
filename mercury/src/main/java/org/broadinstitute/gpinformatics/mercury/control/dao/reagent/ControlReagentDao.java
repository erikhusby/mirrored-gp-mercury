package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for Control Reagents.
 */
@Stateful
@RequestScoped
public class ControlReagentDao extends GenericDao {

    public Map<String, ControlReagent> fetchMapLotToControl(List<String> lots) {
        Map<String, ControlReagent> mapLotToControl = new TreeMap<>();
        for (String lot : lots) {
            mapLotToControl.put(lot, null);
        }
        List<ControlReagent> results = findListByList(ControlReagent.class, Reagent_.lot, lots);
        for (ControlReagent result : results) {
            mapLotToControl.put(result.getLot(), result);
        }
        return mapLotToControl;
    }
}
