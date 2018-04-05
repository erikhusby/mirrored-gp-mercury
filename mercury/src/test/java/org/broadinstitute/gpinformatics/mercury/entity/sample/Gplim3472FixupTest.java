package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Change the MetadataSource for some samples from BSP to MERCURY to allow them to be accessioned for CRSP.
 */
public class Gplim3472FixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction utx;

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @BeforeMethod(groups = TestGroups.FIXUP)
    public void setUp() throws Exception {
        if (userBean == null) {
            return;
        }
        utx.begin();
    }

    @AfterMethod(groups = TestGroups.FIXUP)
    public void tearDown() throws Exception {
        if (userBean == null) {
            return;
        }
        utx.commit();
    }

    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void fixSampleDataSource() {
        userBean.loginOSUser();
        List<String> sampleIds = Arrays.asList("SM-7C8NU", "SM-7C8NV", "SM-7C8NW", "SM-7C8NX", "SM-7C8NY", "SM-7C8NZ");
        List<MercurySample> samples = mercurySampleDao.findBySampleKeys(sampleIds);
        for (MercurySample sample : samples) {
            // Make sure the initial conditions are as expected
            assertThat(sample.getMetadataSource(), equalTo(MercurySample.MetadataSource.BSP));
            System.out.println(
                    String.format("Changing MetadataSource for %s from BSP to MERCURY", sample.getSampleKey()));
            sample.setMetadataSource(MercurySample.MetadataSource.MERCURY);
        }
        mercurySampleDao.persist(new FixupCommentary(
                "GPLIM-3472 changing MetadataSource from BSP to MERCURY to allow for clinical accessioning"));
        System.out.println("Fixed " + samples.size() + " samples.");
    }
}
