package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.FIXUP)
public class LabVesselFixupTest extends Arquillian {

    @Inject
    UserTransaction utx;

    @Inject
    LabVesselDao labVesselDao;

    /**
     * Use test deployment here to talk to the actual jira
     *
     */
    @Deployment
    public static WebArchive buildMercuryWar() {

        /*
         * If the need comes to utilize this fixup in production, change the buildMercuryWar parameters accordingly
         */
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @BeforeMethod(groups = TestGroups.FIXUP)
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();
    }

    @AfterMethod(groups = TestGroups.FIXUP)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.commit();
    }


    @Test(groups = TestGroups.FIXUP, enabled = false)
    public void updateNullDatesToJan1() throws Exception {

        List<LabVessel> fixupVessels = labVesselDao.findList(LabVessel.class, LabVessel_.createdOn, null);

        Calendar specificDate = new GregorianCalendar(2013, 0, 1);

        Date janOne = specificDate.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
        System.out.println(formatter.format(janOne));

        for(LabVessel badDateVessel : fixupVessels) {
            badDateVessel.setCreatedOn(janOne);
        }
    }
}
