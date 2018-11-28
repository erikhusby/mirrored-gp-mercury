package org.broadinstitute.gpinformatics.athena.control.dao.products;


import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;

@Test(groups = {TestGroups.STUBBY})
@Dependent
public class ProductFamilyDaoTest extends StubbyContainerTest {

    public ProductFamilyDaoTest(){}

    @Inject
    private ProductFamilyDao dao;


    public void testSingle() {
        // not sure if fixture data has been loaded into our test database yet, so this is just a smoke test
        ProductFamily productFamily = dao.find(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName());
        Assert.assertNotNull(productFamily);
    }

    public void testAll() {
        List<ProductFamily> productFamilies = dao.findAll();
        Assert.assertNotNull(productFamilies);
        Assert.assertTrue(!productFamilies.isEmpty());
    }

}
