package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVesselFixupTest;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
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

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

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


    /**
     * Updates the run barcode for one or more sequencing runs
     */
    @Test(enabled = false)
    public void fixupRunBarcodeGplim3159() throws IOException {
        userBean.loginOSUser();
        Map<String,String> runNameToRunBarcode = buildRunNameToRunBarcodeMap(
                "141031_SL-HCC_0483_AFCHAAVEADXX\tHAAVEADXX141031\n"
                + "141031_SL-HCC_0484_BFCHAB3DADXX\tHAB3DADXX141031\n"
                + "141031_SL-HCD_0319_AFCHAHFBADXX\tHAHFBADXX141031\n"
                + "141031_SL-HCD_0320_BFCHAHHWADXX\tHAHHWADXX141031\n"
                + "141031_SL-HDC_0518_AFCHAAYDADXX\tHAAYDADXX141031\n"
                + "141031_SL-HDC_0519_BFCHAAW2ADXX\tHAAW2ADXX141031\n"
                + "141031_SL-HDE_0484_AHAAV9ADXX\tHAAV9ADXX141031\n"
                + "141031_SL-HDE_0485_BHAAVAADXX\tHAAVAADXX141031\n"
                + "141031_SL-HDF_0532_AHAB6KADXX\tHAB6KADXX141031\n"
                + "141031_SL-HDF_0533_BHAB32ADXX\tHAB32ADXX141031\n"
                + "141031_SL-HDG_0469_AHAAY7ADXX\tHAAY7ADXX141031\n"
                + "141031_SL-HDG_0470_BHAHW6ADXX\tHAHW6ADXX141031\n"
                + "141031_SL-HDH_0502_AHAB3AADXX\tHAB3AADXX141031\n"
                + "141031_SL-HDH_0503_BHAAWCADXX\tHAAWCADXX141031");
        if (runNameToRunBarcode.isEmpty()) {
            throw new RuntimeException("No runs to fixup.");
        }
        System.out.println("About to fixup " + runNameToRunBarcode.size() + " runs");
        for (Map.Entry<String, String> runNameRunBarcodeEntry : runNameToRunBarcode.entrySet()) {
            updateRunBarcode(runNameRunBarcodeEntry.getKey(),runNameRunBarcodeEntry.getValue());
        }
        // master does not have FixupCommentary yet
        illuminaSequencingRunDao.flush();
    }

    /**
     * Parses a possibly multi-line string of the format "runName runBarcode\n"
     * into a map where the key is
     * the run name and the value is the run barcode
     * @param runNameToBarcodeText text like
     * 141031_SL-HDF_0533_BHAB32ADXX	HAB32ADXX141031
     * 141031_SL-HDG_0469_AHAAY7ADXX	HAAY7ADXX141031
     */
    private Map<String,String> buildRunNameToRunBarcodeMap(String runNameToBarcodeText) throws IOException {
        Map<String,String> runNameToRunBarcode = new HashMap<>();
        BufferedReader reader = new BufferedReader(new StringReader(
                runNameToBarcodeText
        ));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split("\\s+");
            runNameToRunBarcode.put(words[0].trim(),words[1].trim());
        }
        return runNameToRunBarcode;
    }

    /**
     * Updates the run barcode for the given run name
     */
    private void updateRunBarcode(@Nonnull String runName,@Nonnull String correctedRunBarcode) {
        IlluminaSequencingRun sequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        if (sequencingRun == null) {
            throw new RuntimeException("Could not find run " + runName);
        }
        System.out.println("Changing run barcode on " + runName + " from " + sequencingRun.getRunBarcode() + " to " + correctedRunBarcode);
        sequencingRun.setRunBarcode(correctedRunBarcode);
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
            String originalRunDirectory = run.getRunDirectory();
            assertThat(originalRunDirectory, startsWith("/crsp/illumina/"));

            String modifiedRunDirectory = originalRunDirectory.replaceFirst("/crsp/illumina/", "/crsp/qa/illumina/");
            assertThat(modifiedRunDirectory, startsWith("/crsp/qa/illumina/"));
            assertThat(modifiedRunDirectory.length(), equalTo(run.getRunDirectory().length() + "/qa".length()));

            run.setRunDirectory(modifiedRunDirectory);

            System.out.println(String.format("Updated run directory for sequencing run named %s from %s to %s",
                    run.getRunName(), originalRunDirectory, modifiedRunDirectory));
        }

        illuminaSequencingRunDao.flush();
        System.out.println("Updates flushed to database.");
    }

    @Test(enabled = false)
    public void fixupGplim3224() {
        userBean.loginOSUser();
        String[] runNames = {"141031_SL-HDG_0469_AHAAY7ADXX",
                "141031_SL-HDF_0533_BHAB32ADXX",
                "141031_SL-HDF_0532_AHAB6KADXX",
                "141031_SL-HDE_0485_BHAAVAADXX",
                "141031_SL-HDE_0484_AHAAV9ADXX",
                "141031_SL-HDC_0519_BFCHAAW2ADXX",
                "141031_SL-HDC_0518_AFCHAAYDADXX",
                "141031_SL-HCD_0320_BFCHAHHWADXX",
                "141031_SL-HCD_0319_AFCHAHFBADXX",
                "141031_SL-HCC_0484_BFCHAB3DADXX",
                "141031_SL-HCC_0483_AFCHAAVEADXX",
                "141031_SL-HDH_0503_BHAAWCADXX",
                "141031_SL-HDH_0502_AHAB3AADXX",
                "141031_SL-HDG_0470_BHAHW6ADXX"};
        for (String runName : runNames) {
            IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
            if (illuminaSequencingRun == null) {
                throw new RuntimeException("Failed to find " + runName);
            }
            System.out.println("Updating " + runName);
            illuminaSequencingRun.setRunDirectory(illuminaSequencingRun.getRunDirectory().replace(
                    "/crsp/qa/illumina", "/crsp/illumina"));
        }
        illuminaSequencingRunDao.persist(new FixupCommentary("GPLIM-3224 Fixup CRSP QA run folder"));
        illuminaSequencingRunDao.flush();
    }

    @Test(enabled = false)
    public void fixupRunDirectoryGplim3288() {
        String[] runNames = {
                "141221_SL-HDJ_0508_AHBG13ADXX",
                "141221_SL-HCD_0352_BFCHBFPTADXX",
                "141221_SL-HCC_0522_BFCHBG2TADXX",
                "141221_SL-HDC_0547_AFCHBFRVADXX",
                "141220_SL-HDB_0563_BFCHBG2CADXX",
                "141220_SL-HDJ_0507_BHBFMHADXX",
                "141220_SL-HDG_0510_AHBFN0ADXX",
                "141220_SL-HDF_0565_AHBFMGADXX",
                "141220_SL-HDE_0522_BHBFMUADXX",
                "141220_SL-HDE_0521_AHBG2KADXX",
                "141221_SL-HCD_0351_AFCHBG29ADXX",
                "141221_SL-HDB_0564_AFCHBFPPADXX",
                "141221_SL-HCC_0521_AFCHBG24ADXX",
                "141221_SL-HDB_0565_BFCHBG19ADXX",
                "141221_SL-HDC_0548_BFCHBFPDADXX",
                "141220_SL-HDH_0535_AHBFMYADXX",
                "141220_SL-HCD_0350_BFCHBFN1ADXX",
                "141220_SL-HCC_0519_AFCHBFRGADXX",
                "141220_SL-HDC_0545_AFCHBG65ADXX",
                "141220_SL-HDJ_0506_AHBG6AADXX",
                "141220_SL-HDF_0566_BHBFMMADXX",
                "141220_SL-HDG_0511_BHBG2BADXX",
                "141220_SL-HCD_0349_AFCHBFMWADXX",
                "141220_SL-HCC_0520_BFCHBFN7ADXX",
                "141220_SL-HDB_0562_AFCHBFMVADXX",
                "141220_SL-HDC_0546_BFCHBFRLADXX",
                "141220_SL-HBW_0503_AHBFRJADXX",
                "141220_SL-HBW_0504_BHBG5VADXX"
        };
        for (String runName : runNames) {
            IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runName);
            String machineName = runName.split("_")[1];
            String badRunDirectory = run.getRunDirectory();
            String goodRunDirectory = badRunDirectory
                    .replace("/crsp/illumina2/proc/", String.format("/crsp/illumina2/proc/%s/", machineName));

            run.setRunDirectory(goodRunDirectory);
            System.out.println(String.format("Changed runDirectory for %s from %s to %s", runName, badRunDirectory,
                    goodRunDirectory));
        }
        illuminaSequencingRunDao.persist(new FixupCommentary(
                "Fixing missing machine name in run directory path. See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3288"));
    }

    @Test(enabled = false)
    public void gplim3376FixupFlowcellBarcode() {
        userBean.loginOSUser();
        // Change lab_vessel 1946416 flowcell_barcode to HGKJCADXX and flowcell_type to HiSeq2500Flowcell
        IlluminaFlowcell flowcell = illuminaSequencingRunDao.findById(IlluminaFlowcell.class, 1946416L);
        Assert.assertNotNull(flowcell);
        flowcell.setFlowcellBarcode("HGKJCADXX");
        flowcell.setFlowcellType(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell);

        System.out.println("Updated flowcell " + flowcell.getLabVesselId() +
                           " barcode to " + flowcell.getCartridgeBarcode() +
                           " and type to " + flowcell.getFlowcellType());
        illuminaSequencingRunDao.persist(new FixupCommentary("GPLIM-3376 fixup barcode and flowcell type."));
        illuminaSequencingRunDao.flush();
    }

    @Test(enabled = false)
    public void gplim3627MoveCRSPRuns() {
        userBean.loginOSUser();

        String[] runNames = new String[]{"150612_SL-HDH_0666_AHTCLKADXX", "150612_SL-HDH_0667_BHTCKLADXX"};
        for (String runName : runNames) {
            IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runName);
            String oldRunDirectory = run.getRunDirectory();
            String newRunDirectory = oldRunDirectory.replace("/seq/illumina/", "/crsp/illumina2/");
            run.setRunDirectory(newRunDirectory);
            System.out.println(
                    String.format("Updated run directory for '%s' from '%s' to '%s'", runName, oldRunDirectory,
                            newRunDirectory));
        }
        illuminaSequencingRunDao.persist(new FixupCommentary("GPLIM-3627 correcting run directory for CRSP runs"));
    }

    private void updateRunDirectory(String runName, String oldRunDirectory, String newRunDirectory) {
        IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(runName);
        assertThat(run.getRunDirectory(), equalTo(oldRunDirectory));
        run.setRunDirectory(newRunDirectory);
        System.out.println(String.format("Updated run directory for '%s' from '%s' to '%s'", runName, oldRunDirectory, newRunDirectory));
    }

    @Test(enabled = false)
    public void gplim3996MoveNonCrspRun() {
        userBean.loginOSUser();

        updateRunDirectory("160129_SL-HDF_0760_BH2HV2ADXY", "/crsp/illumina2/proc/SL-HDF/160129_SL-HDF_0760_BH2HV2ADXY",
                "/seq/illumina/proc/SL-HDF/160129_SL-HDF_0760_BH2HV2ADXY");

        illuminaSequencingRunDao.persist(new FixupCommentary("GPLIM-3996 updating run directory for non-CRSP run"));
    }

    @Test(enabled = false)
    public void support2463MoveRunFolder() {
        userBean.loginOSUser();

        updateRunDirectory("170112_SL-HDJ_0843_AH5CL2BCXY", "/seq/illumina/proc/SL-HDJ/170112_SL-HDJ_0843_AH5CL2BCXY",
                "/crsp/illumina2/proc/SL-HDJ/170112_SL-HDJ_0843_AH5CL2BCXY");

        illuminaSequencingRunDao.persist(new FixupCommentary("SUPPORT-2463 moving run folder to crsp folder"));
    }

    @Test(enabled = false)
    public void support2469MoveCrspRunFolders() {
        userBean.loginOSUser();

        updateRunDirectory("170117_SL-HDE_0829_AH5CKTBCXY", "/seq/illumina/proc/SL-HDE/170117_SL-HDE_0829_AH5CKTBCXY",
                "/crsp/illumina2/proc/SL-HDE/170117_SL-HDE_0829_AH5CKTBCXY");

        updateRunDirectory("170117_SL-HDE_0830_BH5TV3BCXY", "/seq/illumina/proc/SL-HDE/170117_SL-HDE_0830_BH5TV3BCXY",
                "/crsp/illumina2/proc/SL-HDE/170117_SL-HDE_0830_BH5TV3BCXY");

        illuminaSequencingRunDao.persist(new FixupCommentary("SUPPORT-2469 updating run directory to crsp directories"));
    }

    @Test(enabled = false)
    public void fixupPo7897() {
        userBean.loginOSUser();
        // storeRunReadStructure is supplying run barcode, but there are two runs with same barcode, so change
        // the unwanted one
        IlluminaSequencingRun illuminaSequencingRun =
                illuminaSequencingRunDao.findByRunName("170222_SL-HXH_0551_AFCHFYL5ALXX");
        System.out.println("Prepending x to duplicate run barcode " + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRun.setRunBarcode("x" + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRunDao.persist(new FixupCommentary("PO-7897 add x to duplicate run barcode"));
    }

    /**
     * storeRunReadStructure is supplying run barcode, but there are two runs with same barcode, so change
     * the unwanted one.
     * This fixup takes input from a file of the following format (ticket ID, run name):
     * PO-7948
     * 170222_SL-HXE_0674_BFCHFVYWALXX
     */
    @Test(enabled = false)
    public void fixupPo7948() throws IOException {
        userBean.loginOSUser();
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("FixupRunBarcode.txt"));
        String jiraTicket = lines.get(0);
        String runName = lines.get(1);
        IlluminaSequencingRun illuminaSequencingRun = illuminaSequencingRunDao.findByRunName(runName);
        System.out.println("Prepending x to duplicate run barcode " + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRun.setRunBarcode("x" + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRunDao.persist(new FixupCommentary(jiraTicket + " add x to duplicate run barcode"));
    }

    @Test(enabled = false)
    public void fixupPo11040() throws Exception{
        userBean.loginOSUser();
        utx.begin();
        updateRunDirectory("171211_SL-HDE_0957_AHYHGVBCXY", "/seq/illumina/proc/SL-HDE/171211_SL-HDE_0957_AHYHGVBCXY",
                "/crsp/illumina2/proc/SL-HDE/171211_SL-HDE_0957_AHYHGVBCXY");
        FixupCommentary fixupCommentary =
                new FixupCommentary("PO-11040 updating run directory for 171211_SL-HDE_0957_AHYHGVBCXY to CRSP folder");
        illuminaSequencingRunDao.persist(fixupCommentary);
        illuminaSequencingRunDao.flush();

        utx.commit();
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateRunFolder.txt,
     * so it can be used for other similar fixups, without writing a new test.  Example contents of the file are:
     * PO-136444 run folder moved after registration
     * 180721_SL-MAC_0453_FC000000000-BV4PM /crsp/illumina2/proc/SL-HDD/run_transfers/BV4PM
     */
    @Test(enabled = false)
    public void fixupPo13644ChangeRunFolder() throws Exception {
        userBean.loginOSUser();

        List<String> sampleUpdateLines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateRunFolder.txt"));

        for(int i = 1; i < sampleUpdateLines.size(); i++) {
            String[] fields = LabVesselFixupTest.WHITESPACE_PATTERN.split(sampleUpdateLines.get(i));
            if(fields.length != 2) {
                throw new RuntimeException("Expected two white-space separated fields in " + sampleUpdateLines.get(i));
            }
            IlluminaSequencingRun run = illuminaSequencingRunDao.findByRunName(fields[0]);

            Assert.assertNotNull(run, fields[0] + " not found");
            final String newRunFolder = fields[1];
            System.out.println("Changing " + run.getRunDirectory() + " to " + newRunFolder);
            run.setRunDirectory(newRunFolder);
        }

        illuminaSequencingRunDao.persist(new FixupCommentary(sampleUpdateLines.get(0)));
        illuminaSequencingRunDao.flush();
    }

    @Test(enabled = false)
    public void fixupPo8033() {
        userBean.loginOSUser();
        // storeRunReadStructure is supplying run barcode, but there are two runs with same barcode, so change
        // the unwanted one
        IlluminaSequencingRun illuminaSequencingRun =
                illuminaSequencingRunDao.findByRunName("170307_SL-HXJ_0555_BFCHFY2JALXX");
        System.out.println("Prepending x to duplicate run barcode " + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRun.setRunBarcode("x" + illuminaSequencingRun.getRunBarcode());
        illuminaSequencingRunDao.persist(new FixupCommentary("PO-8033 add x to duplicate run barcode"));
    }
}
