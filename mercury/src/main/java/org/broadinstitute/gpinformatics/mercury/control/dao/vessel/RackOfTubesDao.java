package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for racks of tubes
 */
@Stateful
@RequestScoped
public class RackOfTubesDao extends GenericDao {

    public TubeFormation findByDigest(String digest) {
        return findSingle(TubeFormation.class, RackOfTubes_.label, digest);
    }
}
