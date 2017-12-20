package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.run.InfiniumArchiver;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselFixupTest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

/**
 * "Test" to resubmit Infinium starter messages.
 */
@Test(groups = TestGroups.FIXUP)
public class InfiniumRunFinderFixupTest extends Arquillian {

    @Inject
    private InfiniumRunFinder infiniumRunFinder;

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private UserBean userBean;

    @Inject
    private InfiniumArchiver infiniumArchiver;

    @Inject
    private UserTransaction userTransaction;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * This test reads its parameters from a file, testdata/ResubmitInfiniumRuns.txt.  Example contents of the file are:
     * 201299370054 R07C01
     * 200834530003 R07C01
     */
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

    /**
     * Add INFINIUM_ARCHIVED event to ~1000 chips that were archived by GAP (or if it's DEV, to chips that don't
     * exist in the dev directory).
     */
    @Test(enabled = false)
    public void testGplim5110() throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException {
        userBean.loginOSUser();
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(Calendar.DAY_OF_YEAR, -10);
        List<Pair<String, Boolean>> chipsToArchive = infiniumArchiver.findChipsToArchive(20000, gregorianCalendar.getTime());

        userTransaction.begin();
        int i = 1;
        Date previousEventDate = null;
        long disambiguator = 1L;
        for (Pair<String, Boolean> stringBooleanPair : chipsToArchive) {
            if (stringBooleanPair.getRight()) {
                if (infiniumStarterConfig.getDeploymentConfig() == PROD) {
                    continue;
                } else {
                    File baseDataDir = new File(infiniumStarterConfig.getDataPath());
                    File dataDir = new File(baseDataDir, stringBooleanPair.getLeft());
                    if (dataDir.exists()) {
                        continue;
                    }
                }
            }
            // else assume GAP has archived it (or it's dev and the data directory exists)
            LabVessel chip = labVesselDao.findByIdentifier(stringBooleanPair.getKey());
            Date eventDate = new Date();
            if (Objects.equals(eventDate, previousEventDate)) {
                disambiguator++;
            } else {
                disambiguator = 1L;
            }
            chip.addInPlaceEvent(new LabEvent(LabEventType.INFINIUM_ARCHIVED, eventDate, LabEvent.UI_EVENT_LOCATION,
                    disambiguator, userBean.getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME));
            previousEventDate = eventDate;
            labVesselDao.flush();
            labVesselDao.clear();
            if (i % 100 == 0) {
                System.out.println("Marked " + i + " chips");
            }
            i++;
        }
        labVesselDao.persist(new FixupCommentary("GPLIM-5110 mark chips archived by GAP"));
        userTransaction.commit();
    }

}