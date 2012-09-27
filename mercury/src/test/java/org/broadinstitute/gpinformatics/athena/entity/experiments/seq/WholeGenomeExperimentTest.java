package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.AlignerType;
import org.broadinstitute.gpinformatics.mercury.boundary.CoverageAndAnalysisInformation;
import org.broadinstitute.gpinformatics.mercury.boundary.WholeGenomePass;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Set;

import static org.testng.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/30/12
 * Time: 10:20 AM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class WholeGenomeExperimentTest {

    private WholeGenomeExperiment wholeGenomeExperiment;
    private ExperimentRequestSummary experimentRequestSummary;

    @BeforeMethod
    public void setUp() throws Exception {
        experimentRequestSummary = new ExperimentRequestSummary(
                "An Experiment Title", new Person("athena"),
                new Date(),
                ExperimentType.WholeGenomeSequencing
        );
        wholeGenomeExperiment = new WholeGenomeExperiment(experimentRequestSummary);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetCoverageModelTypes() throws Exception {

        Set<CoverageModelType> coverageModelTypeSet = wholeGenomeExperiment.getCoverageModelTypes();
        Assert.assertNotNull(coverageModelTypeSet);
        Assert.assertEquals(coverageModelTypeSet.size(), 2);
        Assert.assertTrue(coverageModelTypeSet.contains(CoverageModelType.LANES));
        Assert.assertTrue(coverageModelTypeSet.contains(CoverageModelType.DEPTH));
        {
            try {
                SeqCoverageModel seqCoverageModel = new PFReadsCoverageModel();
                wholeGenomeExperiment.setSeqCoverageModel(seqCoverageModel);
                fail("Should be invalid coverage type");
            } catch (Exception exp) {
                //Should throw exception
            }
        }
        {
            try {
                SeqCoverageModel seqCoverageModel = new MeanTargetCoverageModel();
                wholeGenomeExperiment.setSeqCoverageModel(seqCoverageModel);
                fail("Should be invalid coverage type");
            } catch (Exception exp) {
                //Should throw exception
            }
        }
        {
            try {
                SeqCoverageModel seqCoverageModel = new TargetCoverageModel();
                wholeGenomeExperiment.setSeqCoverageModel(seqCoverageModel);
                fail("Should be invalid coverage type");
            } catch (Exception exp) {
                //Should throw exception
            }
        }
    }

    @Test
    public void testAlignerType() throws Exception {

        //Should default to BWA
        AlignerType alignerType = wholeGenomeExperiment.getAlignerType();
        Assert.assertEquals(alignerType, AlignerType.BWA);

        //Can no longer set to MAQ directly
        try {
            wholeGenomeExperiment.setAlignerType(AlignerType.MAQ);
        } catch (IllegalArgumentException iae) {
            // should throw exception,  MAQ no longer allowed to be set directly
        }

        wholeGenomeExperiment.setAlignerType(AlignerType.BWA);
        Assert.assertEquals(wholeGenomeExperiment.getAlignerType(), AlignerType.BWA);

        //Test can still init an experiment (via a pass) with MAQ though.
        WholeGenomePass wholeGenomePass1 = new WholeGenomePass();
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        coverageAndAnalysisInformation.setAligner(AlignerType.MAQ);
        wholeGenomePass1.setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);

        WholeGenomeExperiment wholeGenomeExperiment1 = new WholeGenomeExperiment(experimentRequestSummary, wholeGenomePass1);
        Assert.assertEquals(wholeGenomeExperiment1.getAlignerType(), AlignerType.MAQ);

    }

    @Test
    public void testEquals() throws Exception {

        WholeGenomeExperiment wholeGenomeExperiment2 = new WholeGenomeExperiment(experimentRequestSummary);
        Assert.assertEquals(wholeGenomeExperiment, wholeGenomeExperiment);
        Assert.assertEquals(wholeGenomeExperiment, wholeGenomeExperiment2);

        Assert.assertEquals(wholeGenomeExperiment, wholeGenomeExperiment2);

        Assert.assertFalse(wholeGenomeExperiment2.equals(""));

        wholeGenomeExperiment2.setDiseaseName("BlueFlu");
        Assert.assertEquals(wholeGenomeExperiment, wholeGenomeExperiment2);

    }

    @Test
    public void testHashCode() throws Exception {

        WholeGenomeExperiment wholeGenomeExperimenta = new WholeGenomeExperiment(experimentRequestSummary);

        WholeGenomeExperiment wholeGenomeExperimentb = new WholeGenomeExperiment(experimentRequestSummary);
        Assert.assertEquals(wholeGenomeExperimenta.hashCode(), wholeGenomeExperimentb.hashCode());

        wholeGenomeExperimentb.setDiseaseName("BlueFlu");
        Assert.assertEquals(wholeGenomeExperimenta.hashCode(), wholeGenomeExperimentb.hashCode());

    }

    @Test
    public void testToString() throws Exception {
        WholeGenomeExperiment wholeGenomeExperiment2 = new WholeGenomeExperiment(experimentRequestSummary);
        Assert.assertEquals("SeqExperimentRequest{seqCoverageModel=null, organism=null, referenceSequenceName=null, experimentRequestSummary=Name{name='An Experiment Title'}, samples=[], experimentType=WholeGenomeSequencing}",
                wholeGenomeExperiment2.toString());
    }


}
