package org.broadinstitute.gpinformatics.mercury.control.dao.project;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data Access Object for projects.
 */
@Stateful
@RequestScoped
public class ProjectPlanDao extends GenericDao {
}
