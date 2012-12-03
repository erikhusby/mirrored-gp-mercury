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

import java.util.List;

/**
 * Data Access for DesignedReagents
 */
public class ReagentDesignDao extends GenericDao {

    public List<ReagentDesign> findAll() {
        return super.findAll(ReagentDesign.class);
    }

    public ReagentDesign findById(Long id){
        return findSingle(ReagentDesign.class, ReagentDesign_.reagentDesignId,id);
    }
}
