package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access Object for racks of tubes
 */
@Stateful
@RequestScoped
public class RackOfTubesDao extends GenericDao {

    public RackOfTubes findByDigest(String digest) {
        return findSingle(RackOfTubes.class, RackOfTubes_.digest, digest);
    }

    public RackOfTubes getByLabel(String rackLabel) {
        return findSingle(RackOfTubes.class, RackOfTubes_.label, rackLabel);
    }
}
