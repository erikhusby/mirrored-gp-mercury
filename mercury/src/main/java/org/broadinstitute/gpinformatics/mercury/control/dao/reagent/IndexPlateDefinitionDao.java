package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

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

@Stateful
@RequestScoped
public class IndexPlateDefinitionDao extends GenericDao {

    public IndexPlateDefinition findByName(String plateDefinitionName) {
        return findSingle(IndexPlateDefinition.class, IndexPlateDefinition_.definitionName, plateDefinitionName);
    }

    /**
     * Returns the index plates instantiated from the index plate definition.
     * @param plateDefinitionName
     * @return list of index plates, or null if the indexPlateDefinition doesn't exist.
     */
    public List<StaticPlate> findByPlateDefintion(String plateDefinitionName) {
        IndexPlateDefinition indexPlateDefinition = findSingle(IndexPlateDefinition.class,
                IndexPlateDefinition_.definitionName, plateDefinitionName);
        if (indexPlateDefinition != null) {
            return new ArrayList<>(indexPlateDefinition.getPlateInstances());
        }
        return null;
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
