package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

/**
 * Data Access Object for sequencing runs
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class IlluminaSequencingRunDao extends GenericDao{

    public IlluminaSequencingRun findByRunName(String runName) {
        return findSingle(IlluminaSequencingRun.class, IlluminaSequencingRun_.runName, runName);
    }

    /**
     * Find IlluminaSequencingRuns matching runBarcode. This method could return more then one run.
     * To guarantee a single result call findByRunName (if you know the run name).
     *
     * @param runBarcode the barcode for the run you are searching
     *
     * @return A Collection of IlluminaSequencingRuns with barcode matching runBarcode.
     */
    public Collection<IlluminaSequencingRun> findByBarcode(String runBarcode) {
        return findList(IlluminaSequencingRun.class, IlluminaSequencingRun_.runBarcode, runBarcode);
    }

    public List<IlluminaSequencingRun> findAllOrderByRunName() {
        return findAll(IlluminaSequencingRun.class, new GenericDaoCallback<IlluminaSequencingRun>() {
            @Override
            public void callback(CriteriaQuery<IlluminaSequencingRun> criteriaQuery, Root<IlluminaSequencingRun> root) {
                criteriaQuery.orderBy(getCriteriaBuilder().asc(root.get(IlluminaSequencingRun_.runName)));
            }
        });
    }
}
