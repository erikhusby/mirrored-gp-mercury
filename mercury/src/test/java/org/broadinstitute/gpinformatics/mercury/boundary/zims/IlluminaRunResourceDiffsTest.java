package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ImportFromSquidTest;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * A test to compare IlluminaRunResource output from different deployments, e.g. to verify a change.
 */
@Test(groups = TestGroups.STANDARD)
public class IlluminaRunResourceDiffsTest extends Arquillian {

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(groups = TestGroups.STANDARD, enabled = false)
    public void testCompareAll() {
        List<IlluminaSequencingRun> illuminaSequencingRuns = illuminaSequencingRunDao.findAll(
                IlluminaSequencingRun.class);
        for (IlluminaSequencingRun illuminaSequencingRun : illuminaSequencingRuns) {
            // Exclude runs created by tests
            if (!illuminaSequencingRun.getRunName().contains("Flowcell") &&
                    !illuminaSequencingRun.getRunName().toLowerCase().contains("test")) {
                compareRun(illuminaSequencingRun);
            }
        }
    }

    @Test(groups = TestGroups.STANDARD, enabled = false)
    public void testCompareOne() {
        IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(
                "140122_SL-HBV_0297_AFCH79B6ADXX");
        compareRun(illuminaSequencingRun);
    }

    private void compareRun(IlluminaSequencingRun illuminaSequencingRun) {
        System.out.println("Comparing run " + illuminaSequencingRun.getRunName());
        try {
            String localRun = IlluminaRunResourceLiveTest.getZimsIlluminaRunString(
                    new URL(ImportFromSquidTest.TEST_MERCURY_URL + "/"),
                    illuminaSequencingRun.getRunName());
            String referenceRun = IlluminaRunResourceLiveTest.getZimsIlluminaRunString(
                    new URL("http://mercurydev:8080/Mercury/"),
                    illuminaSequencingRun.getRunName());
            JSONCompareResult jsonCompareResult = JSONCompare.compareJSON(referenceRun, localRun,
                    JSONCompareMode.LENIENT);
            if (jsonCompareResult.failed()) {
                System.out.println(jsonCompareResult.getMessage());
            }
        } catch (Throwable e) {
            System.out.println(e);
        }
    }
}
