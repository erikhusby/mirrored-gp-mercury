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
            chip.addInPlaceEvent(new LabEvent(LabEventType.INFINIUM_ARCHIVED, new Date(), LabEvent.UI_EVENT_LOCATION,
                    1L, userBean.getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME));
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

    /**
     * Add INFINIUM_ARCHIVED event to chips that were recently changed to Forward to GAP = N.  Driven by a file
     * generated from this query:
     * SELECT
     *     *
     * FROM
     *     lab_vessel lv
     *     INNER JOIN lab_event le
     *         ON  le.in_place_lab_vessel = lv.lab_vessel_id
     * WHERE
     *     le.lab_event_type = 'INFINIUM_XSTAIN'
     *     AND NOT EXISTS (
     *                 SELECT
     *                     1
     *                 FROM
     *                     lab_event le2
     *                 WHERE
     *                     le2.in_place_lab_vessel = lv.lab_vessel_id
     *                     AND le2.lab_event_type = 'INFINIUM_ARCHIVED'
     *         )
     * ORDER BY
     *     lv.created_on;
     * The file has a chip barcode on each line.  The fixup will not mark a chip as archived if it doesn't have a
     * corresponding zip file, unless you override this by adding a space and F (for force) after a barcode.
     */
    @Test(enabled = false)
    public void testSupport6117() throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException, IOException {
        userBean.loginOSUser();
        userTransaction.begin();

        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("MarkArchived.txt"));
        int i = 1;
        for (String line : lines) {
            // Allow lines to be commented out
            if (line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split("\\s");
            String chipBarcode = fields[0];
            String force = null;
            if (fields.length == 2) {
                force = fields[1];
            }

            // Don't mark archived if no archive file, unless "force" is specified
            File baseArchiveDir = new File(infiniumStarterConfig.getArchivePath());
            File zipFile = new File(baseArchiveDir, chipBarcode + ".zip");
            if (!zipFile.exists()) {
                System.out.println("No archive file for " + zipFile.getAbsolutePath());
                File baseDataDir = new File(infiniumStarterConfig.getDataPath());
                File dataDir = new File(baseDataDir, chipBarcode);
                System.out.println(dataDir.getAbsolutePath() +  (dataDir.exists() ? " exists" : " doesn't exist"));
                if (!Objects.equals(force, "F")) {
                    continue;
                }
            }

            LabVessel chip = labVesselDao.findByIdentifier(chipBarcode);
            if (chip.getInPlaceLabEvents().stream().noneMatch(
                    labEvent -> labEvent.getLabEventType() == LabEventType.INFINIUM_ARCHIVED)) {
                System.out.println("Marking " + chip.getLabel() + " archived");
                chip.addInPlaceEvent(new LabEvent(LabEventType.INFINIUM_ARCHIVED, new Date(), LabEvent.UI_EVENT_LOCATION,
                        1L, userBean.getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME));
            }
            // Flush and clear periodically, to avoid running out of memory
            if (i % 100 == 0) {
                labVesselDao.flush();
                labVesselDao.clear();
                System.out.println("Marked " + i + " chips");
            }
            i++;
        }
        labVesselDao.persist(new FixupCommentary("SUPPORT-6117 mark chips archived by GAP"));
        userTransaction.commit();
    }

}