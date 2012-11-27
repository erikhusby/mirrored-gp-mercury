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
import org.broadinstitute.gpinformatics.mercury.entity.reagent.BaitReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.BaitReagent_;

import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * Data Access object for BaitReagent
 */
public class BaitReagentDao extends GenericDao {
    public BaitReagent findByBaitReagent(BaitReagent bait) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BaitReagent> criteriaQuery = criteriaBuilder.createQuery(BaitReagent.class);
        Root<BaitReagent> root = criteriaQuery.from(BaitReagent.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get(BaitReagent_.targetSet), bait.getTargetSet()));
        criteriaQuery.where(criteriaBuilder.equal(root.get(BaitReagent_.reagentName), bait.getDesignName()));
        criteriaQuery.where(criteriaBuilder.equal(root.get(BaitReagent_.lot), bait.getManufacturerId()));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}
