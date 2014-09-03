package org.broadinstitute.gpinformatics.mercury.control.dao.manifest;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

/**
 * DAO for ManifestSessions.
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class ManifestSessionDao extends GenericDao {
    public ManifestSession find(long id) {
        return findById(ManifestSession.class, id);
    }
}
