package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType_;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@LocalBean
@RequestScoped
public class AnalysisTypeDao extends GenericDao implements BusinessKeyFinder<AnalysisType> {
    public List<AnalysisType> findAll() {
        return findAll(AnalysisType.class);
    }

    @Override
    public AnalysisType findByBusinessKey(String businessKey) {
        return findSingle(AnalysisType.class, AnalysisType_.name, businessKey);
    }
}
