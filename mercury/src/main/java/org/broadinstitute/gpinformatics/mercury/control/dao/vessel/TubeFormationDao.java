package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access Object for tube formations
 */
@Stateful
@RequestScoped
public class TubeFormationDao extends GenericDao {

    public TubeFormation findByDigest(String digest) {
        return findSingle(TubeFormation.class, RackOfTubes_.label, digest);
    }

    public List<TubeFormation> findByLabels(List<String> labels) {
        return findListByList(TubeFormation.class, TubeFormation_.label, labels);
    }
}
