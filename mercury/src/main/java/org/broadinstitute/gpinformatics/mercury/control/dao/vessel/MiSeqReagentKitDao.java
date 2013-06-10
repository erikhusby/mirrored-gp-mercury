package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MiSeqReagentKit_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for MiSeq Reagent Kits
 */
@Stateful
@RequestScoped
public class MiSeqReagentKitDao extends GenericDao {
    public MiSeqReagentKit findByBarcode(String barcode) {
        return findSingle(MiSeqReagentKit.class, MiSeqReagentKit_.label, barcode);
    }
}
