package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselFixupTest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * "Test" to resubmit Infinium starter messages.
 */
@Test(groups = TestGroups.FIXUP)
public class InfiniumRunFinderFixupTest {

    @Inject
    private InfiniumRunFinder infiniumRunFinder;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private UserBean userBean;

    @Inject
    private UserTransaction userTransaction;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void testSupport2710() {
        try {
            userBean.loginOSUser();

            List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("ResubmitInfiniumRuns.txt"));
            StaticPlate chip = null;
            for (String line : lines) {

                String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(line);
                if (fields.length != 2) {
                    throw new RuntimeException("Expected two white-space separated fields in " + line);
                }
                String barcode = fields[0];
                String positionName = fields[1];

                VesselPosition vesselPosition = VesselPosition.getByName(positionName);
                if (vesselPosition == null) {
                    throw new RuntimeException("Failed to find position " + positionName);
                }

                // If the input file is sorted by chip, avoid fetching multiple times
                if (chip == null || !chip.getLabel().equals(barcode)) {
                    chip = staticPlateDao.findByBarcode(barcode);
                    if (chip == null) {
                        throw new RuntimeException("Failed to find chip " + barcode);
                    }
                }

                LabEvent someStartedEvent = null;
                for (LabEvent labEvent : chip.getInPlaceLabEvents()) {
                    if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_SOME_STARTED) {
                        someStartedEvent = labEvent;
                    }
                }
                if (someStartedEvent == null) {
                    throw new RuntimeException("Failed to find " + LabEventType.INFINIUM_AUTOCALL_SOME_STARTED +
                            " for " + chip.getLabel());
                }

                userTransaction.begin();
                infiniumRunFinder.start(chip, vesselPosition, someStartedEvent);
                userTransaction.commit();
            }
        } catch (IOException | NotSupportedException | RollbackException | SystemException |
                HeuristicRollbackException | HeuristicMixedException e) {
            throw new RuntimeException(e);
        }
    }

}