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

package org.broadinstitute.gpinformatics.mercury.boundary.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.List;

/**
 * methods to access and persist ReagentDesigns.
 */
@Stateful
@RequestScoped
public class ReagentDesignManager {
    @Inject
    ReagentDesignDao reagentDesignDao;

    /**
     * Save or update a reagentDesign.
     * @param reagentDesign design to save or update
     */
    public void saveOrEditReagentDesign(ReagentDesign reagentDesign) {
        reagentDesignDao.persist(reagentDesign);
    }

    /**
     * Return single ReagentDesign identified by reagentDesignId, or null if none exists.
     * @param reagentDesignId
     * @return
     */
    public ReagentDesign findById(Long reagentDesignId) {
        return reagentDesignDao.findById(reagentDesignId);
    }

    public List<ReagentDesign> findAll() {
        final List<ReagentDesign> all = reagentDesignDao.findAll();
        return all;
    }
}
