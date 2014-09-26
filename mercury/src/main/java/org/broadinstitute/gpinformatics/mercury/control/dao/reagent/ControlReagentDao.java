package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ControlReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access Object for Control Reagents.
 */
@Stateful
@RequestScoped
public class ControlReagentDao extends GenericDao {

    public List<ControlReagent> fetchByLot(String lot) {
        return findList(ControlReagent.class, Reagent_.lot, lot);
    }
}
