package org.broadinstitute.pmbridge.infrastructure.gap;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/29/12
 * Time: 3:13 PM
 */
public class GenotypingServiceTest {


    @Inject
    GenotypingService genotypingService;

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

        // <product name="HumanCytoSNP-12v1-0_D" display-name="Cyto 12" id="153"/>
        Product product = genotypingService.lookupTechnologyProductById( new Integer( 153 ));
        Assert.assertNotNull(product);
        Assert.assertNotNull( product.getId() );
        Assert.assertEquals(product.getId(), "007");
        Assert.assertEquals(product.getDisplayName(), "Super Chip");
        Assert.assertEquals(product.getName(), "SuperChipCytoSNP-12v1-0_D");

    }
}
