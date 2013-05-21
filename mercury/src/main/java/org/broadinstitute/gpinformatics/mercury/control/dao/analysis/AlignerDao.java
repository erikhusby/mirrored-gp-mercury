package org.broadinstitute.gpinformatics.mercury.control.dao.analysis;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner_;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access for Analysis types.
 */
@Stateful
@LocalBean
@RequestScoped
public class AlignerDao extends GenericDao implements BusinessKeyFinder {
    public List<Aligner> findAll() {
        return findAll(Aligner.class);
    }

    @Override
    public Aligner findByBusinessKey(String businessKey) {
        return findSingle(Aligner.class, Aligner_.name, businessKey);
    }
}
