package org.broadinstitute.gpinformatics.athena.control.dao.samples;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Provides the ability to execute all CRUD operations for the SampleReceiptValidation entity
 */
@Stateful
@RequestScoped
public class SampleReceiptValidationDao extends GenericDao {
}
