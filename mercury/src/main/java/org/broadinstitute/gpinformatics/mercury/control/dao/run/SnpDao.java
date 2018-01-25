package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for Snp entities.
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SnpDao extends GenericDao {

    public Map<String, Snp> findByRsIds(Collection<String> rsIds) {
        Map<String, Snp> mapRsIdToSnp = new TreeMap<>();
        for (String barcode : rsIds) {
            mapRsIdToSnp.put(barcode, null);
        }
        List<Snp> results = findListByList(Snp.class, Snp_.rsId, rsIds);
        for (Snp result : results) {
            mapRsIdToSnp.put(result.getRsId(), result);
        }
        return mapRsIdToSnp;
    }

}
