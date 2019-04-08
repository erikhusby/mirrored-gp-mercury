package org.broadinstitute.gpinformatics.mercury.control.run;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class InfniumArchiverFixupTest extends Arquillian {

    @Inject
    private UserBean userBean;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        // Change dataSourceEnvironment to "prod", but leave deployment as DEV; copy yaml infiniumStarter dataPath,
        // decodeDataPath and archivePath from PROD to DEV.  This seems safer than using PROD, which may run ETL etc.
        return DeploymentBuilder.buildMercuryWar(DEV, "prod");
    }

    /**
     * This test reads its parameters from file mercury/src/test/resources/testdata/ArchiveInfiniumChips.txt .
     * Each line is a chip barcode, which can be preceded by # to skip over previously archived chips when restarting
     * a failed run.
     * To get reasonable performance, this test must be run on a Linux host (the copying and zipping is much slower
     * through a Windows share).  Modify src\test\resources-dev\arquillian.xml as follows:
     * 	<defaultProtocol type="Servlet 3.0">
     *    <property name="host">mercuryfb.broadinstitute.org</property>
     *  </defaultProtocol>
     *  <container qualifier="dev" default="true">
     *    <configuration>
     *      <property name="managementAddress">mercuryfb.broadinstitute.org</property>
     *      <property name="managementPort">9990</property>
     *      <property name="username">Administrator</property>
     *      <property name="password">TBD</property>
     *   </configuration>
     * </container>
     */
    @Test(enabled = false)
    public void fixupGplim5599ArchiveOnPrem() {
        userBean.loginOSUser();

        try {
            List<String> barcodes = IOUtils.readLines(VarioskanParserTest.getTestResource("ArchiveInfiniumChips.txt"));

            for (String barcode : barcodes) {
                // allow commenting out of lines
                if (barcode.startsWith("#")) {
                    continue;
                }
                if (InfiniumArchiver.archiveChip(barcode, infiniumStarterConfig)) {
                    LabVessel chip = labVesselDao.findByIdentifier(barcode);
                    chip.addInPlaceEvent(new LabEvent(LabEventType.INFINIUM_ARCHIVED, new Date(), LabEvent.UI_EVENT_LOCATION,
                            1L, userBean.getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME));
                    labVesselDao.flush();
                    labVesselDao.clear();
                }
            }
            // No FixupCommentary, this is not out of the ordinary
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
