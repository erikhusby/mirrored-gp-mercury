package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObjectFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.CoverageType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.CoverageType_;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@LocalBean
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class CoverageTypeDao extends GenericDao implements BusinessObjectFinder<CoverageType> {
    public List<CoverageType> findAll() {
        return findAll(CoverageType.class);
    }

    @Override
    public CoverageType findByBusinessKey(String businessKey) {
        return findSingle(CoverageType.class, CoverageType_.name, businessKey);
    }
}
