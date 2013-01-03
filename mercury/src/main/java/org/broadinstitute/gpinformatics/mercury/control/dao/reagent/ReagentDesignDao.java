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

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.List;

/**
 * Data Access for DesignedReagents
 */
@Stateful
@RequestScoped
public class ReagentDesignDao extends GenericDao {

    public List<ReagentDesign> findAll() {
        return super.findAll(ReagentDesign.class);
    }

    public ReagentDesign findByBusinessKey(String value) {
        return findSingle(ReagentDesign.class, ReagentDesign_.designName, value);
    }

    public List<ReagentDesign> findByBusinessKey(final Collection businessKeys) {
        return findAll(ReagentDesign.class,
                new GenericDao.GenericDaoCallback<ReagentDesign>() {
                    @Override
                    public void callback(CriteriaQuery<ReagentDesign> cq, Root<ReagentDesign> root) {
                        cq.where(root.get(ReagentDesign_.designName).in(businessKeys));
                    }
                });
    }
}
