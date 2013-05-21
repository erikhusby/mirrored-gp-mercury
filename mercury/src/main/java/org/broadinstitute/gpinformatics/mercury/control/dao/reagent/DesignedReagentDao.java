/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
public class DesignedReagentDao extends GenericDao {
    public DesignedReagent findByBusinessKey(String key) {
        return findSingle(DesignedReagent.class, DesignedReagent_.name, key);
    }
}
