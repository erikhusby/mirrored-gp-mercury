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
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.Date;

@Stateful
@RequestScoped
public class DesignedReagentDao extends GenericDao {
    public DesignedReagent findByBusinessKey(String key) {
        return findSingle(DesignedReagent.class, DesignedReagent_.name, key);
    }

    public DesignedReagent findByReagentLotDesignAndExpiration(ReagentDesign reagentDesign, String lot, Date expiration) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<DesignedReagent> criteriaQuery = criteriaBuilder.createQuery(DesignedReagent.class);

        Root<DesignedReagent> root = criteriaQuery.from(DesignedReagent.class);
        Join<DesignedReagent, ReagentDesign> reagentDesigns = root.join(DesignedReagent_.reagentDesign);

        criteriaQuery.where(
                criteriaBuilder.equal(reagentDesigns.get(ReagentDesign_.reagentDesignId), reagentDesign.getReagentDesignId()),
                criteriaBuilder.equal(root.get(DesignedReagent_.lot), lot),
                (expiration == null ? criteriaBuilder.isNull(root.get(DesignedReagent_.expiration)) :
                        criteriaBuilder.equal(root.get(DesignedReagent_.expiration), expiration)));
        try {
            return getEntityManager().createQuery(criteriaQuery).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}
