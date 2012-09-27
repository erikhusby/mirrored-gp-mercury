package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;

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
