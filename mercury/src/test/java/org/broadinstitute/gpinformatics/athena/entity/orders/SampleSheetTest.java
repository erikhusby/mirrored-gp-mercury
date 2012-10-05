package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/3/12
 * Time: 12:12 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class SampleSheetTest {

    private List<BillableSample> sixBspSamplesNoDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<BillableSample> fourBspSamplesWithDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACGC,SM-2AB1B,SM-2ACJC,SM-2ACGC",
                    new HashSet<BillableItem>() ) ;

    private List<BillableSample> sixMixedSamples = createSampleList("SM-2ACGC,SM2ABDD,SM2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<BillableSample> nonBspSamples = createSampleList("SSM-2ACGC1,SM--2ABDDD,SM-2AB,SM-2AB1B,SM-2ACJCACB,SM-SM-SM",
                    new HashSet<BillableItem>() ) ;

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    //TODO
//    @Test
//    public void testGetUniqueParticipantCount() throws Exception {
//
//    }

    @Test
    public void testGetUniqueSampleCount() throws Exception {

        SampleSheet sampleSheet = new SampleSheet(sixBspSamplesNoDupes);
        Assert.assertEquals(sampleSheet.getUniqueSampleCount(), 6);

        sampleSheet = new SampleSheet(fourBspSamplesWithDupes);
        Assert.assertEquals(sampleSheet.getUniqueSampleCount(), 4);

    }

    @Test
    public void testGetTotalSampleCount() throws Exception {

        SampleSheet sampleSheet = new SampleSheet(sixBspSamplesNoDupes);
        Assert.assertEquals(sampleSheet.getTotalSampleCount(), 6);

        sampleSheet = new SampleSheet(fourBspSamplesWithDupes);
        Assert.assertEquals(sampleSheet.getTotalSampleCount(), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {
        SampleSheet sampleSheet = new SampleSheet(fourBspSamplesWithDupes);
        Assert.assertEquals(sampleSheet.getDuplicateCount(), 2);
    }

    //TODO
//    @Test
//    public void testGetDiseaseNormalCounts() throws Exception {
//
//    }

    //TODO
//    @Test
//    public void testGetGenderCount() throws Exception {
//
//    }

    @Test
    public void testAreAllSampleBSPFormat() throws Exception {
        SampleSheet sampleSheet = new SampleSheet(fourBspSamplesWithDupes);
        Assert.assertTrue(sampleSheet.areAllSampleBSPFormat());

        sampleSheet = new SampleSheet(sixBspSamplesNoDupes);
        Assert.assertTrue(sampleSheet.areAllSampleBSPFormat());

        sampleSheet = new SampleSheet(nonBspSamples);
        Assert.assertFalse(sampleSheet.areAllSampleBSPFormat());

        sampleSheet = new SampleSheet(sixMixedSamples);
        Assert.assertFalse(sampleSheet.areAllSampleBSPFormat());

    }


    public static List<BillableSample>  createSampleList( String sampleListStr, HashSet<BillableItem> billableItems) {
        List<BillableSample> orderSamples = new ArrayList<BillableSample>();
        String [] sampleArray = sampleListStr.split(",");
        for ( String sampleName : sampleArray) {
            BillableSample billableSample = new BillableSample(sampleName);
            billableSample.setComment("athenaComment");
            for ( BillableItem billableItem : billableItems ) {
                billableSample.addBillableItem(billableItem);
            }
            orderSamples.add(billableSample);
        }
        return orderSamples;
    }
}
