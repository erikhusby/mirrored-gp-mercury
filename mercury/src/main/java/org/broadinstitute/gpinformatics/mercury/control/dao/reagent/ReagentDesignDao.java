/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessKeyable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Data Access for DesignedReagents.
 */
@Stateful
@LocalBean
@RequestScoped
public class ReagentDesignDao extends GenericDao implements BusinessKeyFinder<ReagentDesign> {
    public List<ReagentDesign> findAll() {
        return super.findAll(ReagentDesign.class);
    }

    public List<ReagentDesign> findAllCurrent() {
        return super.findAll(ReagentDesign.class);
    }

    @Override
    public ReagentDesign findByBusinessKey(String businessKey) {
        return findSingle(ReagentDesign.class, ReagentDesign_.designName, businessKey);
    }

    public ReagentDesign findBaitsByBusinessKey(String value) {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder();

        final CriteriaQuery<ReagentDesign> query = criteriaBuilder.createQuery(ReagentDesign.class);
        Root<ReagentDesign> root = query.from(ReagentDesign.class);
        Predicate namePredicate = criteriaBuilder.equal(root.get(ReagentDesign_.designName), value);

        query.where(namePredicate);
        return getEntityManager().createQuery(query).getSingleResult();
    }
}
