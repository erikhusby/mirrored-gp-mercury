package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting_;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.CriteriaInClauseCreator;
import org.broadinstitute.gpinformatics.infrastructure.jpa.JPASplitter;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ArraysQcDao {

    @PersistenceContext(type = PersistenceContextType.EXTENDED, unitName = "metrics_pu")
    private EntityManager entityManager;

    public List<ArraysQc> findByBarcodes(List<String> chipWellBarcodes) {
        if( chipWellBarcodes == null || chipWellBarcodes.isEmpty() ) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(chipWellBarcodes, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<ArraysQc> criteria = cb.createQuery(ArraysQc.class);
                Root<ArraysQc> root = criteria.from(ArraysQc.class);
                criteria.select(root).where(cb.and(
                        root.get(ArraysQc_.chipWellBarcode).in(parameterList)),
                        cb.equal(root.get(ArraysQc_.isLatest), 1));
                return entityManager.createQuery(criteria);
            }
        });
    }

    public Map<String, ArraysQc> findMapByBarcodes(List<String> chipWellBarcodes) {
        List<ArraysQc> arraysQcList = findByBarcodes(chipWellBarcodes);
        Map<String, ArraysQc> mapWellBarcodeToMetric = new HashMap<>();
        for (ArraysQc arraysQc : arraysQcList) {
            mapWellBarcodeToMetric.put(arraysQc.getChipWellBarcode(), arraysQc);
        }
        return mapWellBarcodeToMetric;
    }

    public List<ArraysQcBlacklisting> findBlacklistByBarcodes(List<String> chipWellBarcodes) {
        if( chipWellBarcodes == null || chipWellBarcodes.isEmpty() ) {
            return Collections.emptyList();
        }
        return JPASplitter.runCriteriaQuery(chipWellBarcodes, new CriteriaInClauseCreator<String>() {
            @Override
            public Query createCriteriaInQuery(Collection<String> parameterList) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<ArraysQcBlacklisting> criteria = cb.createQuery(ArraysQcBlacklisting.class);
                Root<ArraysQcBlacklisting> root = criteria.from(ArraysQcBlacklisting.class);
                criteria.select(root).where(
                        root.get(ArraysQcBlacklisting_.chipWellBarcode).in(parameterList));
                return entityManager.createQuery(criteria);
            }
        });
    }

    public Map<String, ArraysQcBlacklisting> findBlacklistMapByBarcodes(List<String> chipWellBarcodes) {
        List<ArraysQcBlacklisting> arraysQcBlacklist = findBlacklistByBarcodes(chipWellBarcodes);
        Map<String, ArraysQcBlacklisting> mapWellBarcodeToMetric = new HashMap<>();
        for (ArraysQcBlacklisting blacklist : arraysQcBlacklist) {
            mapWellBarcodeToMetric.put(blacklist.getChipWellBarcode(), blacklist);
        }
        return mapWellBarcodeToMetric;
    }
}
