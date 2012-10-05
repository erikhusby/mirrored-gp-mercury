package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;

@Test(enabled = false)
public class ProductFamilyDaoTest extends ContainerTest {

    @Inject
    private ProductFamilyDao dao;


    public void test() {

        // not sure if fixture data has been loaded into our test database yet, so this is just a smoke test
        dao.find(ProductFamily.ProductFamilyName.GENERAL_PRODUCTS);

    }

}
