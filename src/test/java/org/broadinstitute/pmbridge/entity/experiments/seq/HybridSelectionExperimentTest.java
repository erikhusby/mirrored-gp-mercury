package org.broadinstitute.pmbridge.entity.experiments.seq;

import junit.framework.Assert;
import org.broad.squid.services.TopicService.*;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Date;
import java.util.Set;

import static org.broadinstitute.pmbridge.TestGroups.UNIT;
import static org.testng.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/30/12
 * Time: 4:14 PM
 */
@Test(groups = {UNIT})
public class HybridSelectionExperimentTest {
    private HybridSelectionExperiment hybridSelectionExperiment;
    private ExperimentRequestSummary experimentRequestSummary;
    private Long nonDefaultId = HybridSelectionExperiment.DEFAULT_BAIT_SET_ID + 1L;


    @BeforeMethod
    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary  (
                new Person("pmbridge", RoleType.PROGRAM_PM),
                new Date(),
                PlatformType.GSP
        );
        hybridSelectionExperiment = new HybridSelectionExperiment(experimentRequestSummary);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }


    @Test
    public void testGetCoverageModelTypes() throws Exception {

        Set<CoverageModelType> coverageModelTypeSet = hybridSelectionExperiment.getCoverageModelTypes();
        Assert.assertNotNull(coverageModelTypeSet);
        Assert.assertEquals(coverageModelTypeSet.size(), 3);
        Assert.assertTrue(coverageModelTypeSet.contains(CoverageModelType.LANES));
        Assert.assertTrue( coverageModelTypeSet.contains( CoverageModelType.TARGETCOVERAGE) );
        Assert.assertTrue( coverageModelTypeSet.contains( CoverageModelType.MEANTARGETCOVERAGE) );

        {
            try {
                SeqCoverageModel seqCoverageModel = new DepthCoverageModel(BigInteger.ZERO);
                hybridSelectionExperiment.setSeqCoverageModel( seqCoverageModel );
                fail("Should be invalid coverage type");
            } catch (Exception exp ) {
                //Should throw exception
            }
        }
        {
            try {
                SeqCoverageModel seqCoverageModel = new PFReadsCoverageModel(BigInteger.ZERO);
                hybridSelectionExperiment.setSeqCoverageModel( seqCoverageModel );
                fail("Should be invalid coverage type");
            } catch (Exception exp ) {
               //Should throw exception
            }
        }


    }


    @Test
    public void testBaitSetID() throws Exception {

        // Check against the default
        Assert.assertEquals( HybridSelectionExperiment.DEFAULT_BAIT_SET_ID, hybridSelectionExperiment.getBaitSetID());

        // Set it to another non-default value
        hybridSelectionExperiment.setBaitSetID(nonDefaultId);
        Assert.assertEquals( nonDefaultId, hybridSelectionExperiment.getBaitSetID() );

        // Set it back to default
        hybridSelectionExperiment.setBaitSetID(HybridSelectionExperiment.DEFAULT_BAIT_SET_ID);    }


    @Test
    public void testAlignerType() throws Exception {

        //Should default to TopHat
        AlignerType alignerType = hybridSelectionExperiment.getAlignerType();
        Assert.assertEquals( alignerType, AlignerType.BWA );

        //Can not set to MAQ directly
        try{
            hybridSelectionExperiment.setAlignerType(AlignerType.MAQ);
        } catch ( IllegalArgumentException iae ) {
            // should throw exception,  MAQ not allowed to be set directly
        }

        //Test can still init an experiment (via a pass) with MAQ though.
        DirectedPass directedPass = new DirectedPass();
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        coverageAndAnalysisInformation.setAligner( AlignerType.BWA );
        directedPass.setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);

        HybridSelectionExperiment hybridSelectionExperiment1 = new HybridSelectionExperiment(experimentRequestSummary, directedPass);
        Assert.assertEquals(hybridSelectionExperiment1.getAlignerType(), AlignerType.BWA);

    }


    @Test
    public void testEqualsAndHashCode() throws Exception {

        HybridSelectionExperiment hybridSelectionExperiment2 = new HybridSelectionExperiment(experimentRequestSummary);
        Assert.assertTrue( hybridSelectionExperiment.equals( hybridSelectionExperiment2 ) );
        int code  =  hybridSelectionExperiment.hashCode() ;
        int code2 =  hybridSelectionExperiment2.hashCode() ;
        Assert.assertTrue( code == code2 );

        // Change the baitset
        hybridSelectionExperiment2.setBaitSetID( nonDefaultId );
        Assert.assertFalse(hybridSelectionExperiment.equals(hybridSelectionExperiment2));
        Assert.assertFalse( hybridSelectionExperiment.hashCode() == hybridSelectionExperiment2.hashCode()  );

        hybridSelectionExperiment2.setBaitSetID( HybridSelectionExperiment.DEFAULT_BAIT_SET_ID );
        Assert.assertTrue(hybridSelectionExperiment.equals(hybridSelectionExperiment2));
        Assert.assertTrue( hybridSelectionExperiment.hashCode() == hybridSelectionExperiment2.hashCode()  );

    }


    @Test
    public void testToString() throws Exception {

    }
}
