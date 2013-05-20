package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@RequestScoped
public class AnalysisTypeDao extends GenericDao {

    public List<AnalysisType> findAll() {
        return findAll(AnalysisType.class);
    }

    public AnalysisType findByBusinessKey(String value) {
        return findSingle(AnalysisType.class, AnalysisType_.name, value);
    }
}
