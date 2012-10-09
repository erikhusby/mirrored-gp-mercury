package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for Molecular Index
 */
@Stateful
@RequestScoped
public class MolecularIndexDao extends GenericDao {
    public MolecularIndex findBySequence(String sequence) {
        return findSingle(MolecularIndex.class, MolecularIndex_.sequence, sequence);
    }
}
