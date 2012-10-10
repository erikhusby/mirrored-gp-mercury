package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for Strip tubes
 */
@Stateful
@RequestScoped
public class StripTubeDao extends GenericDao {
    public StripTube findByBarcode(String barcode) {
        return findSingle(StripTube.class, StripTube_.label, barcode);
    }
}
