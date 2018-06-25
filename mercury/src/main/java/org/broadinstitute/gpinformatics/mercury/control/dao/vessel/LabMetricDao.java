package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access Object for LabMetric
 */
@Stateful
@RequestScoped
public class LabMetricDao extends GenericDao {
    public List<LabMetric> findByMetricType(LabMetric.MetricType metricType) {
        return findList(LabMetric.class, LabMetric_.metricType, metricType);
    }
}
