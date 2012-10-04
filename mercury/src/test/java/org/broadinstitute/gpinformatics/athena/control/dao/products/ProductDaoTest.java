package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.NoResultException;

@Test
public class ProductDaoTest extends ContainerTest {

    @Inject
    private ProductDao dao;


    public void testFindTopLevelProducts() {

        // just a smoke test for now
        dao.findTopLevelProducts();
    }


    public void testFindByPartNumber() {

        // just a smoke test for now and as such needs to be prepared for NoResultExceptions (nested in EJBExceptions)
        try {
            dao.findProductByPartNumber("GP-GENERAL_PRODUCTS-DNA_EXTRACTION_2012.11.01");
        }
        catch (EJBException ejbx) {
            // Since Daos are @Stateful, a NoResultException that is generated within the Dao method will get
            // wrapped in an EJBException and be thrown back to us.  So if we see an EJBException here that may be
            // okay, provided it's wrapping the expected NoResultException
            Assert.assertTrue(NoResultException.class.isAssignableFrom(ejbx.getCause().getClass()));
        }

    }
}
