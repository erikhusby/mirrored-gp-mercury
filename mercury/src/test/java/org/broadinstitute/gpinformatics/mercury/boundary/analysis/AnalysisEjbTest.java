package org.broadinstitute.gpinformatics.mercury.boundary.analysis;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.AnalysisDataTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AlignerDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This test class works the analysis ejb and the associated DAOs to cover the kinds of queries and saves that
 * will typically be perormed.
 */
@Test(groups = TestGroups.STUBBY, enabled = true)
@Dependent
public class AnalysisEjbTest extends StubbyContainerTest {

    public AnalysisEjbTest(){}

    @Inject
    private AnalysisEjb analysisEjb;

    @Inject
    private AlignerDao alignerDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private ReferenceSequenceDao referenceSequenceDao;

    @Inject
    private AnalysisTypeDao analysisTypeDao;

    @Test(enabled = true)
    public void testAligner() throws Exception {
        // create and save some random aligners.
        Aligner aligner1 = AnalysisDataTestFactory.createTestAligner();
        Aligner aligner2 = AnalysisDataTestFactory.createTestAligner();
        Aligner aligner3 = AnalysisDataTestFactory.createTestAligner();
        Aligner aligner4 = AnalysisDataTestFactory.createTestAligner();
        alignerDao.persist(aligner1);
        alignerDao.persist(aligner2);
        alignerDao.persist(aligner3);
        alignerDao.persist(aligner4);

        boolean added = analysisEjb.addAligner(aligner1.getName());
        Assert.assertFalse(added, "Creating an Aligner with a duplicate name should not add a new one");

        // Remove all the aligners.
        int deleteCount = analysisEjb.removeAligners(
                aligner1.getBusinessKey(), aligner2.getBusinessKey(), aligner3.getBusinessKey(), aligner4.getBusinessKey());
        Assert.assertTrue(deleteCount > 0, "There should have been some Aligners deleted");

        // should be able to add the aligner now.
        added = analysisEjb.addAligner(aligner1.getName());
        Assert.assertNotNull(added, "Aligner should be added here");

        // remove this one.
        deleteCount = analysisEjb.removeAligners(aligner1.getBusinessKey());
        Assert.assertTrue(deleteCount == 1, "Should have been able to remove the newly created aligner");

        // Now find the aligner and then remove it and try to find it again.
        Aligner foundAligner = alignerDao.findByBusinessKey(aligner1.getBusinessKey());
        Assert.assertNull(foundAligner, "Now the aligner should NOT be found");
    }

    @Test(enabled = true)
    public void testAddAnalysisType() throws Exception {
        // create and save some random analysis types.
        AnalysisType analysisType1 = AnalysisDataTestFactory.createTestAnalysisType();
        AnalysisType analysisType2 = AnalysisDataTestFactory.createTestAnalysisType();
        AnalysisType analysisType3 = AnalysisDataTestFactory.createTestAnalysisType();
        AnalysisType analysisType4 = AnalysisDataTestFactory.createTestAnalysisType();
        analysisTypeDao.persist(analysisType1);
        analysisTypeDao.persist(analysisType2);
        analysisTypeDao.persist(analysisType3);
        analysisTypeDao.persist(analysisType4);

        boolean added = analysisEjb.addAnalysisType(analysisType1.getName());
        Assert.assertFalse(added, "Creating an Analysis Type with a duplicate name should not add anything");

        // Remove all the analysis types.
        int deleteCount = analysisEjb.removeAnalysisTypes(
                analysisType1.getBusinessKey(), analysisType2.getBusinessKey(),
                analysisType3.getBusinessKey(), analysisType4.getBusinessKey());
        Assert.assertTrue(deleteCount > 0, "Should have deleted some analysis types");

        // should be able to add the analysis type now.
        added = analysisEjb.addAnalysisType(analysisType1.getName());
        Assert.assertNotNull(added, "Analysis Type should be added here");

        // remove this one.
        analysisEjb.removeAnalysisTypes(analysisType1.getBusinessKey());

        // Now find the type and then remove it and try to find it again.
        AnalysisType foundType = analysisTypeDao.findByBusinessKey(analysisType1.getBusinessKey());
        Assert.assertNull(foundType, "The type should NOT be found");
    }

    @Test(enabled = true)
    public void testAddReferenceSequence() throws Exception {
        // create and save some random reference sequences.
        ReferenceSequence referenceSequence1 = AnalysisDataTestFactory.createTestReferenceSequence(true);
        ReferenceSequence referenceSequence2 = AnalysisDataTestFactory.createTestReferenceSequence(false);
        ReferenceSequence referenceSequence3 = AnalysisDataTestFactory.createTestReferenceSequence(false);
        ReferenceSequence referenceSequence4 = AnalysisDataTestFactory.createTestReferenceSequence(false);
        alignerDao.persist(referenceSequence1);
        alignerDao.persist(referenceSequence2);
        alignerDao.persist(referenceSequence3);
        alignerDao.persist(referenceSequence4);

        boolean added = analysisEjb.addReferenceSequence(referenceSequence1.getName(), referenceSequence1.getVersion(), true);
        Assert.assertFalse(added, "Creating a reference sequence with a duplicate name should not add anything");

        // Now find the sequence with different finders
        ReferenceSequence foundSequence = referenceSequenceDao.findByBusinessKey(referenceSequence1.getBusinessKey());
        Assert.assertNotNull(foundSequence, "The sequence should be found");
        foundSequence = referenceSequenceDao.findByNameAndVersion(referenceSequence1.getName(), referenceSequence1.getVersion());
        Assert.assertNotNull(foundSequence, "The sequence should be found");


        // Remove all the reference sequences.
        int deleteCount = analysisEjb.removeReferenceSequences(
                referenceSequence1.getBusinessKey(), referenceSequence2.getBusinessKey(),
                referenceSequence3.getBusinessKey(), referenceSequence4.getBusinessKey());
        Assert.assertTrue(deleteCount > 0, "Should have deleted the reference sequences");

        // Should be able to add the reference sequence now and also reset the current one
        added = analysisEjb.addReferenceSequence(referenceSequence1.getName(), referenceSequence1.getVersion(), true);
        Assert.assertNotNull(added, "Reference Sequence should be added here");
        foundSequence = referenceSequenceDao.findByNameAndVersion(referenceSequence1.getName(), referenceSequence1.getVersion());
        Assert.assertNotNull(foundSequence, "The sequence should be found");

        String newVersion = referenceSequence1.getVersion()+referenceSequence1.getVersion();
        added = analysisEjb.addReferenceSequence(referenceSequence1.getName(), newVersion);
        Assert.assertTrue(added);
        ReferenceSequence notCurrentReferenceSequence = referenceSequenceDao.findByNameAndVersion(referenceSequence1.getName(), referenceSequence1.getVersion());
        Assert.assertFalse(notCurrentReferenceSequence.isCurrent(), "Should no longer be the current reference sequence");
        ReferenceSequence currentReferenceSequence = referenceSequenceDao.findByNameAndVersion(referenceSequence1.getName(), newVersion);
        Assert.assertNotNull(currentReferenceSequence, "Should have been found");
        Assert.assertTrue(currentReferenceSequence.isCurrent(), "Should be the current sequence");

        // Remove this one.
        deleteCount = analysisEjb.removeReferenceSequences(referenceSequence1.getBusinessKey());
        Assert.assertTrue(deleteCount > 0, "Should have deleted the reference sequence");

        // Now find the sequence and then remove it and try to find it again.
        foundSequence = referenceSequenceDao.findByBusinessKey(referenceSequence1.getBusinessKey());
        Assert.assertNull(foundSequence, "The sequence should NOT be found");
    }

    @Test(enabled = true)
    public void testAddReagentDesign() throws Exception {
        // create and save some random baits.
        ReagentDesign reagentDesign1 = AnalysisDataTestFactory.createTestReagentDesign(ReagentDesign.ReagentType.BAIT);
        ReagentDesign reagentDesign2 = AnalysisDataTestFactory.createTestReagentDesign(ReagentDesign.ReagentType.BAIT);
        ReagentDesign reagentDesign3 = AnalysisDataTestFactory.createTestReagentDesign(ReagentDesign.ReagentType.BAIT);
        ReagentDesign reagentDesign4 = AnalysisDataTestFactory.createTestReagentDesign(ReagentDesign.ReagentType.BAIT);
        reagentDesignDao.persist(reagentDesign1);
        reagentDesignDao.persist(reagentDesign2);
        reagentDesignDao.persist(reagentDesign3);
        reagentDesignDao.persist(reagentDesign4);

        boolean added = analysisEjb.addReagentDesign(reagentDesign1.getName(), ReagentDesign.ReagentType.BAIT);
        Assert.assertFalse(added, "Creating a bait with a duplicate name should not add anything");

        // Remove all the baits.
        int deleteCount = analysisEjb.removeReagentDesigns(
                reagentDesign1.getBusinessKey(), reagentDesign2.getBusinessKey(),
                reagentDesign3.getBusinessKey(), reagentDesign4.getBusinessKey());
        Assert.assertTrue(deleteCount > 0, "Should have deleted the reagent designs");

        // should be able to add the bait now.
        added = analysisEjb.addReagentDesign(reagentDesign1.getName(), ReagentDesign.ReagentType.BAIT);
        Assert.assertNotNull(added, "Bait should be added here");

        // remove this one.
        deleteCount = analysisEjb.removeReagentDesigns(reagentDesign1.getBusinessKey());
        Assert.assertTrue(deleteCount > 0, "Should have deleted the reagent design");

        // Now find the bait and then remove it and try to find it again.
        ReagentDesign foundDesign = reagentDesignDao.findByBusinessKey(reagentDesign1.getBusinessKey());
        Assert.assertNull(foundDesign, "The bait should NOT be found");
    }
}
