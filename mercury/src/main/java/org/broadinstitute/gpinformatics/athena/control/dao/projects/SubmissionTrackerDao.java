package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class SubmissionTrackerDao extends GenericDao {
}
