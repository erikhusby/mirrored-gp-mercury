package org.broadinstitute.gpinformatics.infrastructure.cognos;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIn.isIn;

@Test(groups = TestGroups.STANDARD)
public class OrspProjectDaoTest extends Arquillian {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Inject
    private OrspProjectDao orspProjectDao;

    public void testFindUnknownId() {
        OrspProject project = orspProjectDao.findByKey("ORSP-0");
        assertThat(project, nullValue());
    }

    public void testFindIrb() {
        OrspProject project = orspProjectDao.findByKey("ORSP-2222");
        assertThat(project.getType(), equalTo(RegulatoryInfo.Type.IRB));
    }

    public void testFindNotHumanSubjectsResearch() {
        OrspProject project = orspProjectDao.findByKey("ORSP-524");
        assertThat(project.getType(), equalTo(RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH));
    }

    public void testFindNotEngagedProject() {
        OrspProject project = orspProjectDao.findByKey("ORSP-1000");
        assertThat(project.getType(), equalTo(RegulatoryInfo.Type.ORSP_NOT_ENGAGED));
    }

    public void testFindConsentGroup() {
        OrspProject project = orspProjectDao.findByKey("ORSP-3333");
        assertThat(project, nullValue());
    }

    public void testFindAll() {
        List<OrspProject> orspProjects = orspProjectDao.findAll();
        boolean foundUsable = false;
        boolean foundUnusable = false;
        for (OrspProject orspProject : orspProjects) {
            assertThat(orspProject.getType(), isIn(RegulatoryInfo.Type.values()));
            foundUsable |= orspProject.isUsable();
            foundUnusable |= !orspProject.isUsable();
        }
        assertThat("Should have found at least one project with a usable status", foundUsable);
        assertThat("Should have found at least one project with an unusable status", foundUnusable);
    }
}
