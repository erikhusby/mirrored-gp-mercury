package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Fixups to the SequencingRun entity.
 */
@Test(groups = TestGroups.FIXUP)
public class SequencingRunFixupTest extends Arquillian {

    @Inject
    private IlluminaSequencingRunDao illuminaSequencingRunDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Inject
    private UserBean userBean;

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
}
