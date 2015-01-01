/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.MiSeqReagentKitDao;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixup production Lab Vessel entities
 */
@Test(groups = TestGroups.FIXUP)
public class ReagentKitFixupTest extends Arquillian {
    @Inject
    private MiSeqReagentKitDao miSeqReagentKitDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void renameReagentKit() {
        String barcode = "MS2004527-50V2";
        MiSeqReagentKit labVessel = miSeqReagentKitDao.findByBarcode(barcode);
        if (labVessel != null) {
            labVessel.setLabel(labVessel.getLabel() + "x");
            miSeqReagentKitDao.persist(labVessel);
            miSeqReagentKitDao.flush();
        }
    }
}
