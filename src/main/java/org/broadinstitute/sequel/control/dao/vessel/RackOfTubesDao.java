package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.VesselContainer;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import java.util.List;

/**
 * Data Access Object for racks of tubes
 */
@Stateful
@RequestScoped
public class RackOfTubesDao extends GenericDao {

    public List<RackOfTubes> findByDigest(String digest) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("RackOfTubes.fetchByDigest");
        //noinspection unchecked
        return query.setParameter("digest", digest).getResultList();
    }

    public RackOfTubes getByLabel(String rackLabel) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("RackOfTubes.fetchByLabel");
        return (RackOfTubes) query.setParameter("label", rackLabel).getSingleResult();
    }
}
