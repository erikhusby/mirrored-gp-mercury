package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import java.util.ArrayList;
import java.util.List;

public class LabVesselDao extends GenericDao {
    public List<StaticPlate> findByBarcode(String barcode){
        List<StaticPlate> results = new ArrayList<StaticPlate>();
        results = findListWithWildcard(StaticPlate.class, StaticPlate_.label, barcode);
        return results;
    }
}
