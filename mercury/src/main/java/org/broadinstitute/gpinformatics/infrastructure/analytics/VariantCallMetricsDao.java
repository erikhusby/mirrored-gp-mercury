package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.VariantCallMetric;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.VariantCallMetric_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

@RequestScoped
@Stateful
@TransactionAttribute(TransactionAttributeType.NEVER)
public class VariantCallMetricsDao extends GenericDao {

    public List<VariantCallMetric> findBySampleAlias(List<String> sampleAlias) {
        return findListByList(VariantCallMetric.class, VariantCallMetric_.sampleAlias, sampleAlias);
    }
}
