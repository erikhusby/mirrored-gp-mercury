package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Check that historic runs are still usable by the pipeline.
 */
@Test(groups = TestGroups.STANDARD)
public class IlluminaRunResourceBulkTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Inject
    private IlluminaRunResource illuminaRunResource;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    /**
     * Every sample in each run must have non-null analysisType to be usable by the pipeline.
     */
    @Test(enabled = false)
    public void testHistory() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(2014, Calendar.OCTOBER, 1);
        for (IlluminaSequencingRun illuminaSequencingRun : illuminaSequencingRunDao.findAll(IlluminaSequencingRun.class)) {
            if (illuminaSequencingRun.getRunDate().compareTo(gregorianCalendar.getTime()) > 0) {
                String runName = illuminaSequencingRun.getRunName();
                System.out.println("Fetching " + runName);
                ZimsIlluminaRun zimsIlluminaRun = illuminaRunResource.getRun(runName);
                for (ZimsIlluminaChamber zimsIlluminaChamber : zimsIlluminaRun.getLanes()) {
                    for (LibraryBean libraryBean : zimsIlluminaChamber.getLibraries()) {
                        if (libraryBean.getAnalysisType() == null) {
                            System.out.println("Null analysis type " + libraryBean.getSampleId());
                        }
                        if (libraryBean.getCollaboratorSampleId().contains("null")) {
                            System.out.println(libraryBean.getCollaboratorSampleId());
                        }
                    }
                }
            }
        }
    }

}
