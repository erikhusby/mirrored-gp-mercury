package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to the FlowcellDesignation entity.
 */
@Test(groups = TestGroups.FIXUP)
public class FlowcellDesignationFixupTest extends Arquillian {
    private static final boolean IS_POOL_TEST = true;
    private static final boolean IS_PAIRED_END_READ = true;

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    // Retrofit some designations in order to set Pool Test for the flowcell in Mercury ETL.
    @Test(enabled = false)
    public void gplim4581() throws Exception {
        userBean.loginOSUser();

        // FCT-34666 shows:
        // lane    loading tube loading conc
        // LANE1 	0214585496 	180
        // LANE2 	0214585496 	180
        // LANE3 	0214585496 	180
        // LANE4 	0214585496 	180
        // LANE5 	0214585496 	180
        // LANE6 	0214585496 	180
        // LANE7 	0214585488 	180
        // LANE8 	0214585488 	180

        List<LabVessel> loadingTubes = labVesselDao.findByListIdentifiers(Arrays.asList("0214585496", "0214585488"));
        Assert.assertEquals(loadingTubes.size(), 2);

        String flowcellBarcode = "H7JYKALXX";
        IlluminaFlowcell flowcell = illuminaFlowcellDao.findByBarcode(flowcellBarcode);
        Assert.assertNotNull(flowcell);
        FlowcellDesignation.IndexType indexType = (flowcell.getUniqueIndexes().size() > 1) ?
                FlowcellDesignation.IndexType.DUAL : (flowcell.getUniqueIndexes().size() > 0) ?
                FlowcellDesignation.IndexType.SINGLE : FlowcellDesignation.IndexType.NONE;

        List<Object> entities = new ArrayList<>();
        for (LabVessel loadingTube : loadingTubes) {
            int numberLanes = loadingTube.getLabel().equals("0214585496") ? 6 : 2;
            for (LabEvent loadingEvent : loadingTube.getEvents()) {
                if (loadingEvent.getLabEventType() == LabEventType.DENATURE_TRANSFER) {
                    System.out.println("Creating flowcell designation for loading tube " + loadingTube);
                    entities.add(new FlowcellDesignation(loadingTube, null, loadingEvent, indexType, IS_POOL_TEST,
                            flowcell.getFlowcellType(), numberLanes, 151, new BigDecimal(180), IS_PAIRED_END_READ,
                            FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL));
                }
            }
        }
        Assert.assertFalse(entities.isEmpty());

        entities.add(new FixupCommentary("GPLIM-4581 retrofit pool test designations"));
        illuminaFlowcellDao.persistAll(entities);
        illuminaFlowcellDao.flush();
    }

}
