package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;

import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for Molecular Index
 */
public class MolecularIndexDao extends GenericDao {
    public MolecularIndex findBySequence(String sequence) {
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("MolecularIndex.findBySequence");
        query.setParameter("sequence", sequence);
        MolecularIndex molecularIndex = null;
        try {
            molecularIndex = (MolecularIndex) query.getSingleResult();
        } catch (NoResultException ignored) {
        }
        return molecularIndex;
    }
}
