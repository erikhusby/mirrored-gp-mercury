package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

@Test(groups = TestGroups.STANDARD)
public class CrspPositiveControlsProjectIntegrationTest extends ContainerTest {

    @Inject
    private ResearchProjectDao researchProjectDao;

    private CrspPipelineUtils crspPipelineUtils = new CrspPipelineUtils();

    public void testVerifyThatBuickPositiveControlsProjectHasCliaCapRegulatoryDesignation() {
        ResearchProject positiveControlsProject = researchProjectDao.findByBusinessKey(crspPipelineUtils.getResearchProjectForCrspPositiveControls());

        Assert.assertEquals(positiveControlsProject.getRegulatoryDesignation(),ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);
    }
}
