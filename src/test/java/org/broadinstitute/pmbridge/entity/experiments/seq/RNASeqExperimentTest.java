package org.broadinstitute.pmbridge.entity.experiments.seq;

import junit.framework.Assert;
import org.broad.squid.services.TopicService.AlignerType;
import org.broad.squid.services.TopicService.CoverageAndAnalysisInformation;
import org.broad.squid.services.TopicService.RNASeqPass;
import org.broad.squid.services.TopicService.RNASeqProtocolType;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Set;

import static org.broadinstitute.pmbridge.TestGroups.UNIT;
import static org.testng.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/30/12
 * Time: 2:51 PM
 */
@Test(groups = {UNIT})
public class RNASeqExperimentTest {

    private RNASeqExperiment rnaSeqExperiment;
    private ExperimentRequestSummary experimentRequestSummary;
    private Long nonDefaultRefSeqId = RNASeqExperiment.DEFAULT_REFERENCE_SEQUENCE_ID + 1L;


    @BeforeMethod
    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary  (
                new Person("pmbridge", RoleType.PROGRAM_PM),
                new Date(),
                PlatformType.GSP
        );
        rnaSeqExperiment = new RNASeqExperiment(experimentRequestSummary);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetCoverageModelTypes() throws Exception {

        Set<CoverageModelType> coverageModelTypeSet = rnaSeqExperiment.getCoverageModelTypes();
        Assert.assertNotNull(coverageModelTypeSet);
        Assert.assertEquals(coverageModelTypeSet.size(), 2);
        Assert.assertTrue(coverageModelTypeSet.contains(CoverageModelType.LANES));
        Assert.assertTrue(coverageModelTypeSet.contains(CoverageModelType.PFREADS));
        {
            try {
                SeqCoverageModel seqCoverageModel = new DepthCoverageModel();
                rnaSeqExperiment.setSeqCoverageModel( seqCoverageModel );
                fail("Should be invalid coverage type");
            } catch (Exception exp ) {
                //Should throw exception
            }
        }
        {
            try {
                SeqCoverageModel seqCoverageModel = new MeanTargetCoverageModel();
                rnaSeqExperiment.setSeqCoverageModel( seqCoverageModel );
                fail("Should be invalid coverage type");
            } catch (Exception exp ) {
               //Should throw exception
            }
        }
        {
            try {
                SeqCoverageModel seqCoverageModel = new TargetCoverageModel();
                rnaSeqExperiment.setSeqCoverageModel( seqCoverageModel );
                fail("Should be invalid coverage type");
            } catch (Exception exp ) {
               //Should throw exception
            }
        }

    }

    @Test
    public void testAlignerType() throws Exception {

        //Should default to TopHat
        AlignerType alignerType = rnaSeqExperiment.getAlignerType();
        Assert.assertEquals( alignerType, AlignerType.TOPHAT );

        //Can not set to MAQ directly
        try{
            rnaSeqExperiment.setAlignerType(AlignerType.MAQ);
        } catch ( IllegalArgumentException iae ) {
            // should throw exception,  MAQ not allowed to be set directly
        }

        //Can not set to BWA directly
        try{
            rnaSeqExperiment.setAlignerType(AlignerType.BWA);
        } catch ( IllegalArgumentException iae ) {
            // should throw exception,  BWA no longer allowed to be set directly
        }

        //Test can still init an experiment (via a pass) with MAQ though.
        RNASeqPass rnaSeqPass = new RNASeqPass();
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        coverageAndAnalysisInformation.setAligner( AlignerType.BWA );
        rnaSeqPass.setCoverageAndAnalysisInformation( coverageAndAnalysisInformation );

        RNASeqExperiment rnaSeqExperiment1 = new RNASeqExperiment(experimentRequestSummary, rnaSeqPass);
        Assert.assertEquals(rnaSeqExperiment1.getAlignerType(), AlignerType.BWA);

    }

    @Test
    public void testTranscriptomeReferenceSequenceID() throws Exception {

        // Check against the default
        Assert.assertEquals( RNASeqExperiment.DEFAULT_REFERENCE_SEQUENCE_ID,
                rnaSeqExperiment.getTranscriptomeReferenceSequenceID());

        // Set it to another non-default value
        rnaSeqExperiment.setTranscriptomeReferenceSequenceID(nonDefaultRefSeqId);
        Assert.assertEquals( nonDefaultRefSeqId,
                rnaSeqExperiment.getTranscriptomeReferenceSequenceID());

        // Set it back to default
        rnaSeqExperiment.setTranscriptomeReferenceSequenceID( RNASeqExperiment.DEFAULT_REFERENCE_SEQUENCE_ID );
    }


    @Test
    public void testRNAProtocol() throws Exception {

        Assert.assertEquals( RNASeqExperiment.DEFAULT_RNA_PROTOCOL.value(), rnaSeqExperiment.getRNAProtocol() ) ;
        rnaSeqExperiment.setRNAProtocol(RNASeqProtocolType.D_UTP.value());
        Assert.assertEquals(RNASeqProtocolType.D_UTP.value(), rnaSeqExperiment.getRNAProtocol()) ;
        rnaSeqExperiment.setRNAProtocol(RNASeqExperiment.DEFAULT_RNA_PROTOCOL.value());

    }


    @Test
    public void testEqualsAndHashCode() throws Exception {

        RNASeqExperiment rnaSeqExperiment2 = new RNASeqExperiment(experimentRequestSummary);
        Assert.assertTrue( rnaSeqExperiment.equals( rnaSeqExperiment2 ) );
        Assert.assertTrue( rnaSeqExperiment.hashCode() == rnaSeqExperiment2.hashCode()  );

        // Change the protocol
        rnaSeqExperiment2.setRNAProtocol(RNASeqProtocolType.D_UTP.value());
        Assert.assertFalse(rnaSeqExperiment.equals(rnaSeqExperiment2));
        Assert.assertFalse( rnaSeqExperiment.hashCode() == rnaSeqExperiment2.hashCode()  );

        rnaSeqExperiment2.setRNAProtocol(RNASeqExperiment.DEFAULT_RNA_PROTOCOL.value());
        Assert.assertTrue(rnaSeqExperiment.equals(rnaSeqExperiment2));

        //Change the Transcriptome ref seq Id
        rnaSeqExperiment2.setTranscriptomeReferenceSequenceID( nonDefaultRefSeqId );
        Assert.assertFalse(rnaSeqExperiment.equals(rnaSeqExperiment2));
        Assert.assertFalse( rnaSeqExperiment.hashCode() == rnaSeqExperiment2.hashCode()  );

        rnaSeqExperiment2.setTranscriptomeReferenceSequenceID( RNASeqExperiment.DEFAULT_REFERENCE_SEQUENCE_ID );

        Assert.assertTrue(rnaSeqExperiment.equals(rnaSeqExperiment2));
        Assert.assertTrue( rnaSeqExperiment.hashCode() == rnaSeqExperiment2.hashCode()  );


    }

    @Test
    public void testToString() throws Exception {
        RNASeqExperiment rnaSeqExperiment2 = new RNASeqExperiment(experimentRequestSummary);
        Assert.assertEquals( rnaSeqExperiment2.toString(), "RNASeqExperiment{RNAProtocol=TruSeqTranscriptomeReferenceSequenceID=-1AlignerType=TOPHAT}" );

    }
}
