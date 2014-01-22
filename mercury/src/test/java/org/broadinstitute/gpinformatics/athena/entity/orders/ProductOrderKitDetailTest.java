package org.broadinstitute.gpinformatics.athena.entity.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitTest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderKitDetailTest {

    public static final long ORGANISM_ID = 10L;
    public static final long NUMBER_OF_SAMPLES = 5L;
    private ProductOrderKit testKit;


    @BeforeMethod
    public void setUp() throws Exception {
        testKit = new ProductOrderKit(333L, 242323L);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    public void testKitDetailCreation() throws Exception {

        ProductOrderKitDetail testDetail =
                new ProductOrderKitDetail(NUMBER_OF_SAMPLES, ProductOrderKitTest.kitType, ORGANISM_ID,
                        ProductOrderKitTest.materialInfoDto);

        Assert.assertEquals(testDetail.getNumberOfSamples(), Long.valueOf(NUMBER_OF_SAMPLES));
        Assert.assertEquals(testDetail.getBspMaterialName(), ProductOrderKitTest.materialInfoDto.getBspName());
        Assert.assertEquals(testDetail.getKitTypeDisplayName(), KitType.DNA_MATRIX.getDisplayName());
        Assert.assertEquals(testDetail.getOrganismId(), Long.valueOf(ORGANISM_ID));
        Assert.assertTrue(testDetail.getPostReceiveOptions().isEmpty());
        Assert.assertTrue(StringUtils.isBlank(testDetail.getPostReceivedOptionsAsString(",")));
        Assert.assertNull(testDetail.getProductOrderKit());

        testKit.addKitOrderDetail(testDetail);
        Assert.assertNotNull(testDetail.getProductOrderKit());

        Set<PostReceiveOption> postReceiveOptionSet = new HashSet<>();
        postReceiveOptionSet.add(PostReceiveOption.PICO_RECEIVED);
        postReceiveOptionSet.add(PostReceiveOption.PULSE_FIELD_GEL);

        testDetail.setPostReceiveOptions(postReceiveOptionSet);

        Assert.assertFalse(testDetail.getPostReceiveOptions().isEmpty());

    }
}
