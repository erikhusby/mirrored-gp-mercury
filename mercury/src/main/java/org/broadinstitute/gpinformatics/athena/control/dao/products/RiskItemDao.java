package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.io.Serializable;

@Stateful
@RequestScoped
/**
 *
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.products.Product}s, supporting the browse and CRUD UIs.
 *
 */
public class RiskItemDao extends GenericDao implements Serializable {
}
