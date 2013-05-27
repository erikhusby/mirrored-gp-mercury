package org.broadinstitute.gpinformatics.infrastructure.test.withdb;

import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;

import java.util.UUID;

/**
 * This class is used to create test data for the objects that are used by the analysis team.
 */
public class AnalysisDataTestFactory {
    private static final String TEST_ALIGNER_PREFIX = "ALIGNER-";
    private static final String TEST_ANALYSIS_TYPE_PREFIX = "ANALYSISTYPE-";
    private static final String TEST_REFERENCE_SEQUENCE_PREFIX = "REFSEQ-";
    private static final String TEST_REAGENT_DESIGN_PREFIX = "DESIGN-";

    public static Aligner createTestAligner() {
        return new Aligner(TEST_ALIGNER_PREFIX + UUID.randomUUID());
    }

    public static AnalysisType createTestAnalysisType() {
        return new AnalysisType(TEST_ANALYSIS_TYPE_PREFIX + UUID.randomUUID());
    }

    public static ReferenceSequence createTestReferenceSequence(boolean isCurrent) {
        ReferenceSequence referenceSequence =
                new ReferenceSequence(TEST_REFERENCE_SEQUENCE_PREFIX + UUID.randomUUID(), "v2");
        referenceSequence.setCurrent(isCurrent);
        return referenceSequence;
    }

    public static ReagentDesign createTestReagentDesign(ReagentDesign.ReagentType type) {
        return new ReagentDesign(TEST_REAGENT_DESIGN_PREFIX + UUID.randomUUID(), type);
    }
}
