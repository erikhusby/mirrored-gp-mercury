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

    private List<AthenaSample> sixBspSamplesNoDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<AthenaSample> fourBspSamplesWithDupes = createSampleList("SM-2ACGC,SM-2ABDD,SM-2ACGC,SM-2AB1B,SM-2ACJC,SM-2ACGC",
                    new HashSet<BillableItem>() ) ;

    private List<AthenaSample> sixMixedSamples = createSampleList("SM-2ACGC,SM2ABDD,SM2ACKV,SM-2AB1B,SM-2ACJC,SM-2AD5D",
                    new HashSet<BillableItem>() ) ;

    private List<AthenaSample> nonBspSamples = createSampleList("SSM-2ACGC1,SM--2ABDDD,SM-2AB,SM-2AB1B,SM-2ACJCACB,SM-SM-SM",
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

        SampleSheet sampleSheet = new SampleSheet();

        sampleSheet.setSamples(sixBspSamplesNoDupes);
        Assert.assertEquals(sampleSheet.getUniqueSampleCount(), 6);

        sampleSheet.setSamples(fourBspSamplesWithDupes);
        Assert.assertEquals(sampleSheet.getUniqueSampleCount(), 4);

    }

    @Test
    public void testGetTotalSampleCount() throws Exception {

        SampleSheet sampleSheet = new SampleSheet();

        sampleSheet.setSamples(sixBspSamplesNoDupes);
        Assert.assertEquals(sampleSheet.getTotalSampleCount(), 6);

        sampleSheet.setSamples(fourBspSamplesWithDupes);
        Assert.assertEquals(sampleSheet.getTotalSampleCount(), 6);
    }

    @Test
    public void testGetDuplicateCount() throws Exception {
        SampleSheet sampleSheet = new SampleSheet();
        sampleSheet.setSamples(fourBspSamplesWithDupes);
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
        SampleSheet sampleSheet = new SampleSheet();

        sampleSheet.setSamples(fourBspSamplesWithDupes);
        Assert.assertTrue(sampleSheet.areAllSampleBSPFormat());

        sampleSheet.setSamples(sixBspSamplesNoDupes);
        Assert.assertTrue(sampleSheet.areAllSampleBSPFormat());

        sampleSheet.setSamples(nonBspSamples);
        Assert.assertFalse(sampleSheet.areAllSampleBSPFormat());

        sampleSheet.setSamples(sixMixedSamples);
        Assert.assertFalse(sampleSheet.areAllSampleBSPFormat());

    }


    public static List<AthenaSample>  createSampleList( String sampleListStr, HashSet<BillableItem> billableItems) {
        List<AthenaSample> orderSamples = new ArrayList<AthenaSample>();
        String [] sampleArray = sampleListStr.split(",");
        for ( String sampleName : sampleArray) {
            AthenaSample athenaSample = new AthenaSample(sampleName);
            athenaSample.setComment("athenaComment");
            athenaSample.setBillableItems( billableItems );
            orderSamples.add(athenaSample);
        }
        return orderSamples;
    }
}
