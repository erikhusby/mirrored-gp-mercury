package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ArraysQcDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "metrics_pu")
    private EntityManager entityManager;

    public List<ArraysQc> findByBarcodes(List<String> chipWellBarcodes) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArraysQc> criteria = cb.createQuery(ArraysQc.class);
        Root<ArraysQc> root = criteria.from(ArraysQc.class);
        criteria.select(root).where(root.get(ArraysQc_.chipWellBarcode).in(chipWellBarcodes));
        return entityManager.createQuery(criteria).getResultList();
    }
}
