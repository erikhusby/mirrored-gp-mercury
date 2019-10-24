package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.IndexPlateDefinition_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
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

    public List<StaticPlate> findByBarcodes(@Nonnull Collection<String> barcodes) {
        return findListByList(StaticPlate.class, StaticPlate_.label, barcodes);
    }

    public List<StaticPlate> findByPlateType(StaticPlate.PlateType plateType) {
        return findList(StaticPlate.class, StaticPlate_.plateType, plateType);
    }
    /** Returns an ordered list of the index plate definition names. */
    public List<String> findIndexPlateDefinitionNames() {
        CriteriaQuery<String> criteriaQuery = getCriteriaBuilder().createQuery(String.class);
        Root<IndexPlateDefinition> root = criteriaQuery.from(IndexPlateDefinition.class);
        criteriaQuery.select(root.get(IndexPlateDefinition_.definitionName)).
                orderBy(getCriteriaBuilder().asc(root.get(IndexPlateDefinition_.definitionName)));
        return getEntityManager().createQuery(criteriaQuery).getResultList();
    }
}
