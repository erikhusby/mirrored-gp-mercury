package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Fixups to the SequencingRun entity.
 */
@Test(groups = TestGroups.FIXUP)
public class SequencingRunFixupTest extends Arquillian {

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGplim2628() {
        // storeRunReadStructure is supplying run barcode, but there are two runs with same barcode, so change
        // the unwanted one
        IlluminaSequencingRun illuminaSequencingRun =
                illuminaSequencingRunDao.findByRunName("140321_SL-MAD_0181_FC000000000-A7LC2");
        illuminaSequencingRun.setRunBarcode("x" + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRunDao.flush();
    }

    @Test(enabled = false)
    public void fixupRunDirectoryGplim3160() {
        userBean.loginOSUser();
        Collection<String> runNames = Arrays.asList(
                "141031_SL-HCC_0483_AFCHAAVEADXX",
                "141031_SL-HCC_0484_BFCHAB3DADXX",
                "141031_SL-HCD_0319_AFCHAHFBADXX",
                "141031_SL-HCD_0320_BFCHAHHWADXX",
                "141031_SL-HDC_0518_AFCHAAYDADXX",
                "141031_SL-HDC_0519_BFCHAAW2ADXX",
                "141031_SL-HDE_0484_AHAAV9ADXX",
                "141031_SL-HDE_0485_BHAAVAADXX",
                "141031_SL-HDF_0532_AHAB6KADXX",
                "141031_SL-HDF_0533_BHAB32ADXX",
                "141031_SL-HDG_0469_AHAAY7ADXX",
                "141031_SL-HDG_0470_BHAHW6ADXX",
                "141031_SL-HDH_0502_AHAB3AADXX",
                "141031_SL-HDH_0503_BHAAWCADXX"
        );

        List<IlluminaSequencingRun> runs = illuminaSequencingRunDao
                .findListByList(IlluminaSequencingRun.class, IlluminaSequencingRun_.runName, runNames);

        // The DAO is not touched within this loop, so any exceptions will cause all changes to be rolled back.
        for (IlluminaSequencingRun run : runs) {
            assertThat(run.getRunDirectory(), startsWith("/crsp/illumina/"));
            int initialLength = run.getRunDirectory().length();

            run.setRunDirectory(run.getRunDirectory().replaceFirst("/crsp/illumina/", "/crsp/qa/illumina/"));

            assertThat(run.getRunDirectory(), startsWith("/crsp/qa/illumina/"));
            int updatedLength = run.getRunDirectory().length();
            assertThat(updatedLength, equalTo(initialLength + "/qa".length()));
        }

        illuminaSequencingRunDao.flush();
    }
}
