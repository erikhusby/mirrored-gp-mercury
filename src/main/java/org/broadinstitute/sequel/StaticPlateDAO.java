package org.broadinstitute.sequel;

/**
 * Data Access Object for plates
 */
public class StaticPlateDAO {
    public StaticPlate findByBarcode(String barcode) {
        return new StaticPlate(barcode);
    }
}
