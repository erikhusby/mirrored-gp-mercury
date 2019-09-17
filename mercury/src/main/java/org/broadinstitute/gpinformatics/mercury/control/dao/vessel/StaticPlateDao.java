package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access Object for plates
 */
@Stateful
@RequestScoped
public class StaticPlateDao extends GenericDao {

    public StaticPlate findByBarcode(String barcode) {
        return findSingle(StaticPlate.class, StaticPlate_.label, barcode);
    }

    public List<StaticPlate> findByPlateType(StaticPlate.PlateType plateType) {
        return findList(StaticPlate.class, StaticPlate_.plateType, plateType);
    }
}
