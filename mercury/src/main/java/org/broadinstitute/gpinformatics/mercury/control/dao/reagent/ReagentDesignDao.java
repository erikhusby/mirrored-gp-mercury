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

import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObjectFinder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.Collection;
import java.util.List;

/**
 * Data Access for Designed Reagents.
 */
@Stateful
@LocalBean
@RequestScoped
public class ReagentDesignDao extends GenericDao implements BusinessObjectFinder<ReagentDesign> {
    public List<ReagentDesign> findAll() {
        return super.findAll(ReagentDesign.class);
    }

    @Override
    public ReagentDesign findByBusinessKey(String businessKey) {
        return findSingle(ReagentDesign.class, ReagentDesign_.designName, businessKey);
    }

    public List<ReagentDesign> findByBusinessKeys(Collection<String> businessKeys) {
        return findListByList(ReagentDesign.class, ReagentDesign_.designName, businessKeys);
    }
}
