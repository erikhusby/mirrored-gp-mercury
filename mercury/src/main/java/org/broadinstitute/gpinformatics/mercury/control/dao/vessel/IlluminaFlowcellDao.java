package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.Query;

/**
 * Data Access Object for flowcells
 */
@Stateful
@RequestScoped
public class IlluminaFlowcellDao extends GenericDao {
    public IlluminaFlowcell findByBarcode(String barcode) {
        IlluminaFlowcell illuminaFlowcell = null;
        Query query = this.getThreadEntityManager().getEntityManager().createNamedQuery("IlluminaFlowcell.findByBarcode");
        try {
            illuminaFlowcell = (IlluminaFlowcell) query.setParameter("barcode", barcode).getSingleResult();
        } catch (NoResultException ignored) {
        }
        return illuminaFlowcell;
    }
}
