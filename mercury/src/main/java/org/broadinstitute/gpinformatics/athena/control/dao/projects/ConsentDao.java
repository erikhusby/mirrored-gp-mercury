package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.Consent;
import org.broadinstitute.gpinformatics.athena.entity.project.Consent_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import java.util.List;

/**
 * Provides support to the application for querying Consents
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class ConsentDao extends GenericDao {

    public List<Consent> findByIdentifier(String identifier) {
        return findList(Consent.class, Consent_.identifier, identifier);
    }

}
