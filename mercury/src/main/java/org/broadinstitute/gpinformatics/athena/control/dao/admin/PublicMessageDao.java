/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.control.dao.admin;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.PublicMessage;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.Query;
import java.util.List;

@Stateful
@RequestScoped
public class PublicMessageDao extends GenericDao {
    /**
     * There should be one PublicMessage record at most.
     *
     * @return the PublicMessage or null if none exist.
     */
    public PublicMessage getMessage() {
        List<PublicMessage> publicMessages = findAll(PublicMessage.class);
        if (publicMessages.isEmpty()){
            return null;
        }
        return CollectionUtils.extractSingleton(publicMessages);
    }

    /**
     * Clears all PublicMessages. Ensures there are no PublicMessages persisted to the database.
     */
    public void clearMessage() {
        Query query = getEntityManager().createQuery("delete from PublicMessage");
        query.executeUpdate();
    }
}
