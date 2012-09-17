package org.broadinstitute.sequel.control.dao.run;

import org.broadinstitute.sequel.control.dao.GenericDao;
import org.broadinstitute.sequel.entity.run.IlluminaSequencingRun;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for sequencing runs
 */
@Stateful
@RequestScoped
public class IlluminaSequencingRunDao extends GenericDao{

    public IlluminaSequencingRun findByRunName(String runName) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("IlluminaSequencingRun.findByRunName");
        query.setParameter("runName", runName);
        IlluminaSequencingRun illuminaSequencingRun = null;
        try {
            illuminaSequencingRun = (IlluminaSequencingRun) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return illuminaSequencingRun;
    }
}
