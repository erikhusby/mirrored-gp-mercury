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

package org.broadinstitute.gpinformatics.mercury.control.dao.rapsheet;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Stateful
@RequestScoped
public class ReworkEntryDao extends GenericDao {
    /**
     * Get all the active rework.
     *
     * Active Rework is Rework which has not been put in a bucket.
     * Once it is added to a bucket, it becomes inactive.
     *
     * @return Active rework.
     */
    public Collection<ReworkEntry> getActive() {
        return findList(ReworkEntry.class, ReworkEntry_.activeRework, true);
    }
}
