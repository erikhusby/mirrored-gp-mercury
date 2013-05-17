package org.broadinstitute.gpinformatics.mercury.entity.analysis.fixup;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.boundary.analysis.AnalysisEjb;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.Aligner;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
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
        analysisEjb.addAnalysisType(new AnalysisType("Analysis Type 1"));
        analysisEjb.addAnalysisType(new AnalysisType("Analysis Type 2"));
        analysisEjb.addAnalysisType(new AnalysisType("Analysis Type 3"));
        analysisEjb.addAnalysisType(new AnalysisType("Analysis Type 4"));

        // Add the aligners
        analysisEjb.addAligner(new Aligner("Aligner 1"));
        analysisEjb.addAligner(new Aligner("Aligner 2"));
        analysisEjb.addAligner(new Aligner("Aligner 3"));
        analysisEjb.addAligner(new Aligner("Aligner 4"));
        analysisEjb.addAligner(new Aligner("Aligner 5"));
        analysisEjb.addAligner(new Aligner("Aligner 6"));
        analysisEjb.addAligner(new Aligner("Aligner 7"));
        analysisEjb.addAligner(new Aligner("Aligner 8"));
        analysisEjb.addAligner(new Aligner("Aligner 9"));

        // Add all the reference sequences
        ReferenceSequence referenceSequence = new ReferenceSequence("HG19", "v2");
        referenceSequence.setCurrent(true);
        analysisEjb.addReferenceSequence(referenceSequence);

        analysisEjb.addReferenceSequence(new ReferenceSequence("HG19", "v1"));
        analysisEjb.addReferenceSequence(new ReferenceSequence("HG18", "v1"));
        analysisEjb.addReferenceSequence(new ReferenceSequence("HG18", "v2"));
        analysisEjb.addReferenceSequence(new ReferenceSequence("HG18", "v3"));

        referenceSequence = new ReferenceSequence("HG18", "v4");
        referenceSequence.setCurrent(true);
        analysisEjb.addReferenceSequence(referenceSequence);

        // Add the baits
        analysisEjb.addReagentDesign(new ReagentDesign("Secret Reagent Man", ReagentDesign.ReagentType.BAIT));
        analysisEjb.addReagentDesign(new ReagentDesign("Agent", ReagentDesign.ReagentType.BAIT));
        analysisEjb.addReagentDesign(new ReagentDesign("A Gent", ReagentDesign.ReagentType.BAIT));
    }
}
