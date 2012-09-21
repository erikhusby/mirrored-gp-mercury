package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/29/12
 * Time: 3:13 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class GenotypingServiceTest {

    GenotypingService genotypingService;

    public GenotypingServiceTest() {
    }

    @BeforeMethod
    public void setUp() throws Exception {
        genotypingService = new MockGenotypingService();
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetPlatformRequest() throws Exception {

    }

    @Test
    public void testSaveExperimentRequest() throws Exception {

    }

    @Test
    public void testSubmitExperimentRequest() throws Exception {

    }

    @Test
    public void testGetRequestSummariesByCreator() throws Exception {

    }

    @Test
    public void testLookupTechnologyProductById() throws Exception {

        // <product name="Omni1M"/>
        Platforms platforms = genotypingService.getPlatforms();
        Assert.assertNotNull(platforms);
        Assert.assertFalse(platforms.getPlatforms().isEmpty());
        Products products = platforms.getPlatforms().get(0).getProducts();
        Assert.assertNotNull(products);
        Assert.assertFalse(products.getProducts().isEmpty());
        Product product = products.getProducts().get(0);
        Assert.assertEquals(product.getName(), "Omni1M");
    }
}
