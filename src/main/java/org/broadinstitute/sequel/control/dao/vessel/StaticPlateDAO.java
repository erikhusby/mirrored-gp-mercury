package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.entity.vessel.StaticPlate;

/**
 * Data Access Object for plates
 */
public class StaticPlateDAO {
    public StaticPlate findByBarcode(String barcode) {
        return new StaticPlate(barcode);
    }
}
