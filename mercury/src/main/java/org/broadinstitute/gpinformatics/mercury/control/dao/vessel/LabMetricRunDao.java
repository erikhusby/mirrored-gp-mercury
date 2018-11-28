package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for LabMetricRun
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class LabMetricRunDao extends GenericDao {

    public LabMetricRun findByName(String runName) {
        return findSingle(LabMetricRun.class, LabMetricRun_.runName, runName);
    }

    public List<LabMetricRun> findSameDateRuns(Date date) {
        return findList(LabMetricRun.class, LabMetricRun_.runDate, date);
    }
}
