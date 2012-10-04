package org.broadinstitute.gpinformatics.athena.control.dao.products;

import org.broadinstitute.gpinformatics.athena.control.dao.AthenaGenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
public class ProductDao extends AthenaGenericDao {
    // add method for getting all top level products, we'll definitely need that

    // we might also need a method to filter by ProductFamily or some other criteria but only write that on an
    // as-needed basis
}
