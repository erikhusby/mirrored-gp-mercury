package org.broadinstitute.gpinformatics.mercury.test;

import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.mockito.Mockito;


/**
 * Helper class that sets up
 * test behavior for CRSP
 */
public class CrspControlsTestUtils {

    private ResearchProject crspPositiveControlsResearchProject;

    private ResearchProjectDao mockResearchProjectDao;

    public CrspControlsTestUtils() {
        crspPositiveControlsResearchProject = new ResearchProject();
        crspPositiveControlsResearchProject.setJiraTicketKey("RP-BuickPositiveControls");
        crspPositiveControlsResearchProject.setSynopsis("Positive Controls for Buick");
        mockResearchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(mockResearchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(
                crspPositiveControlsResearchProject);
    }

    public ResearchProject getMockCrspControlsProject() {
        return crspPositiveControlsResearchProject;
    }

    public ResearchProjectDao getMockResearchProjectDao() {
        return mockResearchProjectDao;
    }
}
