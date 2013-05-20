package org.broadinstitute.gpinformatics.mercury.entity.analysis.fixup;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisEjb;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;
import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class AnalysisFixupTest extends Arquillian {

    @Inject
    private AnalysisEjb analysisEjb;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = true)
    public void initializeAllLists() {

        // Add the analysis types
        analysisEjb.addAnalysisType("Analysis Type 1");
        analysisEjb.addAnalysisType("Analysis Type 2");
        analysisEjb.addAnalysisType("Analysis Type 3");
        analysisEjb.addAnalysisType("Analysis Type 4");

        // Add the aligners
        analysisEjb.addAligner("Aligner 1");
        analysisEjb.addAligner("Aligner 2");
        analysisEjb.addAligner("Aligner 3");
        analysisEjb.addAligner("Aligner 4");
        analysisEjb.addAligner("Aligner 5");
        analysisEjb.addAligner("Aligner 6");
        analysisEjb.addAligner("Aligner 7");
        analysisEjb.addAligner("Aligner 8");
        analysisEjb.addAligner("Aligner 9");

        // Add all the reference sequences
        analysisEjb.addReferenceSequence("HG19", "v2", true);
        analysisEjb.addReferenceSequence("HG18", "v1", false);
        analysisEjb.addReferenceSequence("HG18", "v2", false);
        analysisEjb.addReferenceSequence("HG18", "v3", false);
        analysisEjb.addReferenceSequence("HG18", "v4", true);

        // Add the baits
        analysisEjb.addReagentDesign("Secret Reagent Man", ReagentDesign.ReagentType.BAIT);
        analysisEjb.addReagentDesign("Agent", ReagentDesign.ReagentType.BAIT);
        analysisEjb.addReagentDesign("A Gent", ReagentDesign.ReagentType.BAIT);
    }
}
