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

    // todo jmt add instructions from ArquillianRemoteLinux.txt

    @Inject
    private UserBean userBean;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @Inject
    private LabVesselDao labVesselDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGplim5599ArchiveOnPrem() {
        userBean.loginOSUser();

        try {
            List<String> barcodes = IOUtils.readLines(VarioskanParserTest.getTestResource("ArchiveInfiniumChips.txt"));

            for (String barcode : barcodes) {
                if (barcode.startsWith("#")) {
                    continue;
                }
                InfiniumArchiver.archiveChip(barcode, infiniumStarterConfig);
                LabVessel chip = labVesselDao.findByIdentifier(barcode);
                chip.addInPlaceEvent(new LabEvent(LabEventType.INFINIUM_ARCHIVED, new Date(), LabEvent.UI_EVENT_LOCATION,
                        1L, userBean.getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME));
                labVesselDao.flush();
                labVesselDao.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
