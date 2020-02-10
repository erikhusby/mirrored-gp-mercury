package org.broadinstitute.gpinformatics.mercury.entity.hsa.state;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.DemultiplexStateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRunChamber;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.FIXUP)
public class StateFixupTest extends Arquillian {
    @Inject
    private UserTransaction utx;

    @Inject
    private UserBean userBean;

    @Inject
    private DemultiplexStateDao demultiplexStateDao;

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/RemoveRunChamberFromState.txt, so it can
     * be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * GPLIM-4104
     * {State ID},{run name}, {LANE1}
     */
    @Test(enabled = false)
    public void fixupGplim6242RemoveRunChamberFromState() throws Exception {
        userBean.loginOSUser();
        utx.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("RemoveRunChamberFromState.txt"));
        String jiraTicket = lines.get(0);
        for (String data: lines.subList(1, lines.size())) {
            String[] split = data.split(",");
            String stateId = split[0];
            State state = demultiplexStateDao.findById(State.class, Long.valueOf(stateId));
            if (state == null) {
                throw new RuntimeException("Failed to find state with id " + stateId);
            }
            String runName = split[1].trim();
            IlluminaSequencingRun sequencingRun = illuminaSequencingRunDao.findByRunName(runName);
            if (sequencingRun == null) {
                throw new RuntimeException("Failed to find run with name " + runName);
            }
            String laneName = split[2].trim();
            VesselPosition vesselPosition = VesselPosition.getByName(laneName);
            if (vesselPosition == null) {
                throw new RuntimeException("Unknown lane name " + laneName);
            }
            IlluminaSequencingRunChamber sequencingRunChamber = sequencingRun.getSequencingRunChamber(vesselPosition);
            if (sequencingRunChamber == null) {
                throw new RuntimeException("Run " + runName + " doesn't have lane " + laneName);
            }
            System.out.println("Removing " + runName + " " + laneName + " from state " + stateId);
            state.removeRunChamber(sequencingRunChamber);
        }

        demultiplexStateDao.persist(new FixupCommentary(jiraTicket + " removed run chambers."));
        demultiplexStateDao.flush();
        utx.commit();
    }
}
