/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.analytics;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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

    public void testFindBySamples() {
        ProductOrderSample sample1 = new ProductOrderSample("SM-1", new BspSampleData(Collections.singletonMap(
                BSPSampleSearchColumn.BSP_COLLECTION_BARCODE, "SC-10081")));
        ProductOrderSample sample2 = new ProductOrderSample("SM-2", new BspSampleData(Collections.singletonMap(
                BSPSampleSearchColumn.BSP_COLLECTION_BARCODE, "SC-10244")));
        ProductOrderSample sample3 = new ProductOrderSample("SM-2", new BspSampleData(Collections.singletonMap(
                BSPSampleSearchColumn.BSP_COLLECTION_BARCODE, "SC-10364")));

        List<OrspProject> orspProjects = orspProjectDao.findBySamples(Arrays.asList(sample1, sample2, sample3));

        // for SC-10081
        assertThat(orspProjects, hasItem(OrspProjectMatcher.orspProjectWithKey("ORSP-799")));
        assertThat(orspProjects, hasItem(OrspProjectMatcher.orspProjectWithKey("ORSP-1761")));

        // for SC-10244  ORSP-1769 is "Closed" and closed items are not to be returned.
        assertThat(orspProjects, not(hasItem(OrspProjectMatcher.orspProjectWithKey("ORSP-1769"))));
        assertThat(orspProjects, hasItem(OrspProjectMatcher.orspProjectWithKey("ORSP-1733")));

        // for SC-10364
        assertThat(orspProjects, hasItem(OrspProjectMatcher.orspProjectWithKey("ORSP-641")));

        assertThat(orspProjects, hasSize(4));
    }

    private static class OrspProjectMatcher extends BaseMatcher<OrspProject> {

        private String projectKey;

        static OrspProjectMatcher orspProjectWithKey(String projectKey) {
            OrspProjectMatcher matcher = new OrspProjectMatcher();
            matcher.projectKey = projectKey;
            return matcher;
        }

        @Override
        public boolean matches(Object item) {
            OrspProject orspProject = (OrspProject) item;
            return orspProject.getProjectKey().equals(projectKey);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("OrspProject with key: " + projectKey);
        }
    }
}
