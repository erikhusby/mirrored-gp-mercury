package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import java.util.List;

@Test(enabled = false)
public class ProductDaoTest extends ContainerTest {

    @Inject
    private ProductDao dao;


    public void testFindTopLevelProducts() {

        final List<Product> topLevelProducts = dao.findTopLevelProducts();
        Assert.assertNotNull(topLevelProducts);

        // make sure exex is in there
        for (Product product : topLevelProducts) {
            if ("EXOME_EXPRESS-2012.11.01".equals(product.getPartNumber()))
                return;
        }

        Assert.fail("Did not find Exome Express top-level product!");

    }



    public void testFindByPartNumber() {

        final Product product = dao.findByPartNumber("EXOME_EXPRESS-2012.11.01");
        Assert.assertNotNull(product);

        Assert.assertEquals(product.getPartNumber(), "EXOME_EXPRESS-2012.11.01");
        Assert.assertTrue(product.isTopLevelProduct());
        Assert.assertEquals(product.getWorkflowName(), "EXEX-WF-2012.11.01");

        // negative test
        try {
            dao.findByPartNumber("NONEXISTENT PART!!!");
        }
        catch (EJBException ejbx) {
            // Since Daos are @Stateful, a NoResultException that is generated within the Dao method will get
            // wrapped in an EJBException and be thrown back to us.  So if we see an EJBException here that may be
            // okay, provided it's wrapping the expected NoResultException
            Assert.assertTrue(NoResultException.class.isAssignableFrom(ejbx.getCause().getClass()));
        }

    }
}
