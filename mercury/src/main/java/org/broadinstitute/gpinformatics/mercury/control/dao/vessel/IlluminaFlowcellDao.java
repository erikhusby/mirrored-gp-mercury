package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access Object for flowcells
 */
@Stateful
@RequestScoped
public class IlluminaFlowcellDao extends GenericDao {
    public IlluminaFlowcell findByBarcode(String barcode) {
        return findSingle(IlluminaFlowcell.class, IlluminaFlowcell_.label, barcode);
    }

    public List<IlluminaFlowcell> findLikeBarcode(String barcode) {
        return findListWithWildcard(IlluminaFlowcell.class, barcode, false, IlluminaFlowcell_.label);
    }
}
