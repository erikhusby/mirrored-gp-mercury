package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.STANDARD)
public class MercurySampleDaoTest extends Arquillian {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @BeforeMethod
    public void setUp() throws Exception {
        if(mercurySampleDao == null) {
            return;
        }
    }

    @Test(enabled = false, groups = TestGroups.STANDARD)
    public void testFindDupeSamples() throws Exception {

        List<MercurySample> duplicateSamples = mercurySampleDao.findDuplicateSamples();

        assertThat(duplicateSamples.size(), is(equalTo(2904)));
    }
}
