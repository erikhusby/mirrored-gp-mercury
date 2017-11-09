/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Fixup test for repatienting
 */
@Test(groups = TestGroups.FIXUP)
public class SampleMetadataFixupTest extends Arquillian {
    private static final Pattern TAB_PATTERN = Pattern.compile("\\t");

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private UserBean userBean;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void fixupGPLIM3107_repatienting_NA12878_samples() {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        // @formatter:off
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-7482Q", Metadata.Key.PATIENT_ID, "Buick_PV_NA12878_A", "Buick_PV_NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-7482B", Metadata.Key.PATIENT_ID, "Buick_PV_NA12878_B", "Buick_PV_NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-74838", Metadata.Key.PATIENT_ID, "Buick_PV_NA12878_C", "Buick_PV_NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-7482I", Metadata.Key.PATIENT_ID, "Buick_PV_NA12878_D", "Buick_PV_NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-7482L", Metadata.Key.PATIENT_ID, "Buick_PV_NA12878_E", "Buick_PV_NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-74846", Metadata.Key.PATIENT_ID, "Buick_PV_NA12878_F", "Buick_PV_NA12878"));
        // @formatter:on

        userBean.loginOSUser();
        Map<String, Metadata.Key> fixUpErrors = new HashMap<>();
        Map<String, List<MercurySample>> samplesById =
                mercurySampleDao.findMapIdToListMercurySample(fixupItems.keySet());
        for (MetaDataFixupItem fixupItem : fixupItems.values()) {
            List<MercurySample> mercurySamples = samplesById.get(fixupItem.getSampleKey());
            assertThat(mercurySamples.size(), equalTo(1));
            fixUpErrors.putAll(fixupItem.updateMetadataForSample(mercurySamples.get(0)));
        }
        String assertFailureReason =
                String.format("Error updating some or all samples: %s. Please consult server log for more information.",
                        fixUpErrors);
        assertThat(assertFailureReason, fixUpErrors, equalTo(Collections.EMPTY_MAP));
        mercurySampleDao
                .persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3107"));
        mercurySampleDao.flush();
    }

    @Test(enabled = false)
    public void fixupGPLIM_3355_CRSP_ICE_Validation_sample_repatienting() {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-74P3C", Metadata.Key.PATIENT_ID, "NA12878_2", "NA12878_1"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-74P57", Metadata.Key.PATIENT_ID, "NA12878_3", "NA12878_1"));

        String fixupComment = "see https://gpinfojira.broadinstitute.org:8443/jira/browse/GPLIM-3355";
        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    @Test(enabled = false)
    public void fixupGPLIM_3542_BackFill_Buick_Samples() {
        Map<MercurySample, MetaDataFixupItem> fixupItems = new HashMap<>();
        List<MercurySample> mercurySamples = mercurySampleDao
                .findSamplesWithoutMetadata(MercurySample.MetadataSource.MERCURY, Metadata.Key.MATERIAL_TYPE);
        assertThat("No samples found. Has this test already been run?", mercurySamples.size(), not(0));

        for (MercurySample mercurySample : mercurySamples) {
            MetaDataFixupItem fixupItem =
                    new MetaDataFixupItem(mercurySample.getSampleKey(), Metadata.Key.MATERIAL_TYPE, "", "DNA");
            fixupItems.put(mercurySample, fixupItem);
        }
        String fixupComment = "see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3542";
        addMetadataAndValidate(fixupItems, fixupComment);
    }

    @Test(enabled = false)
    public void fixupGPLIM_3585SwapTumorNormal() {

        MercurySample mercurySample74P3A = mercurySampleDao.findBySampleKey("SM-74P3A");
        String fixupComment743A = "Changing Tumor to Normal.  See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3585";
        updateMetadataAndValidate(MetaDataFixupItem
                .mapOf(mercurySample74P3A.getSampleKey(), Metadata.Key.TUMOR_NORMAL, "Tumor", "Normal"),fixupComment743A);


        MercurySample mercurySample74P3U = mercurySampleDao.findBySampleKey("SM-74P3U");
        String fixupComment743U = "Changing Normal to Tumor.  See https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3585";
        updateMetadataAndValidate(MetaDataFixupItem
                .mapOf(mercurySample74P3U.getSampleKey(), Metadata.Key.TUMOR_NORMAL, "Normal", "Tumor"), fixupComment743U);
    }

    @Test(enabled = false)
    public void crsp163Fixup() {
        MercurySample mercurySample = mercurySampleDao.findBySampleKey("SM-74P3Q");
        updateMetadataAndValidate(MetaDataFixupItem
                .mapOf(mercurySample.getSampleKey(), Metadata.Key.PATIENT_ID, "HCC-1143_100% N1", "HCC-1143_100% N"),
                "CRSP-163 repatienting of validation sample (pre-sequencing)");
    }

    @Test(enabled = false)
    public void fixupGPLIM_3840_invalid_value_for_material_type() {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        String oldValue = "whole blood";
        String newValue = MaterialType.WHOLE_BLOOD_WHOLE_BLOOD_FROZEN.getDisplayName();

        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-A19ZB", Metadata.Key.MATERIAL_TYPE, oldValue, newValue));

        String fixupComment = "see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3840";
        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    @Test(enabled = false)
    public void testGPLIM_3913_manifest_date() throws Exception {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-74PDL", Metadata.Key.BUICK_COLLECTION_DATE, "41256", "12/13/2012"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-74PCO", Metadata.Key.BUICK_COLLECTION_DATE, "41290", "01/16/2013"));

        String fixupComment = "see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3913";
        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    /**
     * Excel auto-incremented NA12878 when user was creating manifest.
     */
    @Test(enabled = false)
    public void testGPLIM_4354_NA12878_Excel() throws Exception {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZO", Metadata.Key.PATIENT_ID, "NA12879", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K11", Metadata.Key.PATIENT_ID, "NA12880", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1D", Metadata.Key.PATIENT_ID, "NA12881", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1P", Metadata.Key.PATIENT_ID, "NA12882", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K22", Metadata.Key.PATIENT_ID, "NA12883", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K2E", Metadata.Key.PATIENT_ID, "NA12884", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JYY", Metadata.Key.PATIENT_ID, "NA12885", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZB", Metadata.Key.PATIENT_ID, "NA12886", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZN", Metadata.Key.PATIENT_ID, "NA12887", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZZ", Metadata.Key.PATIENT_ID, "NA12888", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1C", Metadata.Key.PATIENT_ID, "NA12889", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1O", Metadata.Key.PATIENT_ID, "NA12890", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K21", Metadata.Key.PATIENT_ID, "NA12891", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K2D", Metadata.Key.PATIENT_ID, "NA12892", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JYX", Metadata.Key.PATIENT_ID, "NA12893", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZA", Metadata.Key.PATIENT_ID, "NA12894", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZM", Metadata.Key.PATIENT_ID, "NA12895", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZY", Metadata.Key.PATIENT_ID, "NA12896", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1B", Metadata.Key.PATIENT_ID, "NA12897", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1N", Metadata.Key.PATIENT_ID, "NA12898", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1Z", Metadata.Key.PATIENT_ID, "NA12899", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K2C", Metadata.Key.PATIENT_ID, "NA12900", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JYW", Metadata.Key.PATIENT_ID, "NA12901", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZ9", Metadata.Key.PATIENT_ID, "NA12902", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZL", Metadata.Key.PATIENT_ID, "NA12903", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3JZX", Metadata.Key.PATIENT_ID, "NA12904", "NA12878"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-B3K1A", Metadata.Key.PATIENT_ID, "NA12905", "NA12878"));

        String fixupComment = "see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-4354";
        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    /**
     * Physician requested repatienting of samples uploaded through CRSP Portal.
     */
    @Test(enabled = false)
    public void testCRSP_443_Repatient() throws Exception {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-9J5I2", Metadata.Key.PATIENT_ID, "CP-3682", "CP-3681"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-9J5I4", Metadata.Key.PATIENT_ID, "CP-3680", "CP-3679"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-9J5I6", Metadata.Key.PATIENT_ID, "CP-3679", "CP-3680"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-9J5I7", Metadata.Key.PATIENT_ID, "CP-3681", "CP-3683"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-9J5I8", Metadata.Key.PATIENT_ID, "CP-3681", "CP-3682"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-9J5I9", Metadata.Key.PATIENT_ID, "CP-3683", "CP-3681"));

        String fixupComment = "see https://gpinfojira.broadinstitute.org:8443/jira/browse/CRSP-443";
        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    /**
     * Samples were mixed up before receipt at The Broad.
     */
    @Test(enabled = false)
    public void testCrsp475Mixup() throws Exception {
        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-CEMYY", Metadata.Key.PATIENT_ID, "CP-7596", "CP-7595"));
        fixupItems.putAll(MetaDataFixupItem.mapOf("SM-CEMYZ", Metadata.Key.PATIENT_ID, "CP-7595", "CP-7596"));

        String fixupComment = "see https://gpinfojira.broadinstitute.org:8443/jira/browse/CRSP-475";
        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    /**
     * "Tumor" was silently replaced with empty string, so add "Primary".
     */
    @Test(enabled = false)
    public void testSupport3129Fixup() throws Exception {
        List<MercurySample> mercurySamples = mercurySampleDao.findBySampleKeys(Arrays.asList(
                "SM-B3LYZ",
                "SM-B3LZO",
                "SM-CEMGG",
                "SM-CEMH5",
                "SM-CEMHH",
                "SM-CEMI5",
                "SM-CEMI6"));
        Map<MercurySample, MetaDataFixupItem> fixupItems = new HashMap<>();
        for (MercurySample mercurySample : mercurySamples) {
            MetaDataFixupItem fixupItem = new MetaDataFixupItem(mercurySample.getSampleKey(), Metadata.Key.TUMOR_NORMAL,
                    "", "Primary");
            fixupItems.put(mercurySample, fixupItem);
        }
        String fixupComment = "see https://gpinfojira.broadinstitute.org/jira/browse/SUPPORT-3129";
        addMetadataAndValidate(fixupItems, fixupComment);
    }

    /**
     * This test reads its parameters from a file, mercury/src/test/resources/testdata/UpdateSampleMetadata.txt, so
     * it can be used for other similar fixups, without writing a new test.
     * Line 1 is the fixup commentary.
     * Line 2 and subsequent are SampleId\tMetadata.Key\tOld value\tNew value.
     * Example contents of the file are:
     * CRSP-538
     * SM-CEM8Z	TUMOR_NORMAL	NA	Normal
     * SM-CEM9C	TUMOR_NORMAL	NA	Normal
     */
    @Test(enabled = false)
    public void testCrsp538Fixup() throws Exception {
        List<String> lines = IOUtils.readLines(VarioskanParserTest.getTestResource("UpdateSampleMetadata.txt"));
        String fixupComment = lines.get(0);

        Map<String, MetaDataFixupItem> fixupItems = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = TAB_PATTERN.split(line);
            if (fields.length != 4) {
                throw new RuntimeException("Expected four tab separated fields in " + line);
            }
            fixupItems.putAll(MetaDataFixupItem.mapOf(fields[0], Metadata.Key.valueOf(fields[1]), fields[2], fields[3]));
        }

        updateMetadataAndValidate(fixupItems, fixupComment);
    }

    /**
     * Perform actual fixup and validate.
     */
    private void updateMetadataAndValidate(@Nonnull Map<String, MetaDataFixupItem> fixupItems,
                                           @Nonnull String fixupComment) {
        userBean.loginOSUser();
        Map<String, Metadata.Key> fixUpErrors = new HashMap<>();
        Map<String, MercurySample> samplesById = mercurySampleDao.findMapIdToMercurySample(fixupItems.keySet());
        for (MetaDataFixupItem fixupItem : fixupItems.values()) {
            Map<String, Metadata.Key> fixupItemErrors = new HashMap<>();
            MercurySample mercurySample = samplesById.get(fixupItem.getSampleKey());
            fixupItemErrors.putAll(fixupItem.validateOriginalValue(mercurySample));
            if (fixupItemErrors.isEmpty()) {
                fixupItemErrors.putAll(fixupItem.updateMetadataForSample(mercurySample));
            }
            fixUpErrors.putAll(fixupItemErrors);
        }
        String assertFailureReason =
                String.format("Error updating some or all samples: %s. Please consult server log for more information.",
                        fixUpErrors);
        assertThat(assertFailureReason, fixUpErrors, equalTo(Collections.EMPTY_MAP));
        mercurySampleDao.persist(new FixupCommentary(fixupComment));
        mercurySampleDao.flush();

        samplesById = mercurySampleDao.findMapIdToMercurySample(fixupItems.keySet());
        for (MetaDataFixupItem fixupItem : fixupItems.values()) {
            MercurySample mercurySample = samplesById.get(fixupItem.getSampleKey());
            fixUpErrors.putAll(fixupItem.validateUpdatedValue(mercurySample));
        }
        assertFailureReason =
                        String.format("Updated values do not match expected values for some or all samples: %s. Please consult server log for more information.",
                                fixUpErrors);
        assertThat(assertFailureReason, fixUpErrors, equalTo(Collections.EMPTY_MAP));

    }

    /**
     * Perform actual fixup and validate.
     */
    private void addMetadataAndValidate(@Nonnull Map<MercurySample, MetaDataFixupItem> fixupItems,
                                        @Nonnull String fixupComment) {
        userBean.loginOSUser();
        String originalValueDontMatchError =
                "Original value of sample metadata is not what was expected. Key: %s, Expected: %s, Found: %s";

        for (Map.Entry<MercurySample, MetaDataFixupItem> sampleFixupEntry : fixupItems.entrySet()) {
            MercurySample sample = sampleFixupEntry.getKey();
            MetaDataFixupItem fixupItem = sampleFixupEntry.getValue();
            String errorString = null;
            String sampleMetadata = getSampleMetadataValue(fixupItem.getMetadataKey(), sample);
            // Verify the sample does not have this metadata already.
            if (sampleMetadata!=null) {
                errorString =
                        String.format(originalValueDontMatchError, fixupItem.getMetadataKey(),
                                fixupItem.getOldValue(), sampleMetadata);
            }
            // Check for errors and exit if there are any
            assertThat(errorString, nullValue());

            // Verify the updated value is what we expect.
            sample.getMetadata().add(new Metadata(fixupItem.getMetadataKey(), fixupItem.getNewValue()));
            fixupItem.validateUpdatedValue(sample);

            sampleMetadata = getSampleMetadataValue(fixupItem.getMetadataKey(), sample);
            assertThat(String.format("Updated value is not what was expected. Key: %s, Expected: %s, Found: %s",fixupItem.getMetadataKey(),
                                            fixupItem.getNewValue(), sampleMetadata),
                    fixupItem.validateUpdatedValue(sample), is(Collections.EMPTY_MAP));
        }

        mercurySampleDao.persist(new FixupCommentary(fixupComment));
    }

    private String getSampleMetadataValue(Metadata.Key metadataKey, MercurySample sample) {
        for (Metadata sampleMetadata : sample.getMetadata()) {
            if (sampleMetadata.getKey()==metadataKey){
                return sampleMetadata.getValue();
            }
        }
        return null;
    }
}

class MetaDataFixupItem {
    private static Log log = LogFactory.getLog(MetaDataFixupItem.class);

    private final Metadata.Key metadataKey;
    private final String sampleKey;
    private final String oldValue;
    private final String newValue;

    MetaDataFixupItem(String sampleKey, Metadata.Key metadataKey, String oldValue, String newValue) {
        this.sampleKey = sampleKey;
        this.metadataKey = metadataKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Metadata.Key getMetadataKey() {
        return metadataKey;
    }

    public String getOldValue() {
        return oldValue;
    }

    public static Map<String, MetaDataFixupItem> mapOf(String sampleId, Metadata.Key metadataKey,
                                                       String oldValue, String newValue) {
        return ImmutableMap.of(sampleId, new MetaDataFixupItem(sampleId, metadataKey, oldValue, newValue));
    }

    public Map<String, Metadata.Key> updateMetadataForSample(MercurySample mercurySample) {
        Map<String, Metadata.Key> errors = new HashMap<>();
        if (!mercurySample.getSampleKey().equals(sampleKey)) {
            throw new RuntimeException(
                    String.format("Expected sample %s but received %s", sampleKey, mercurySample.getSampleKey()));
        }

        Metadata metadataRecord = findMetadataRecord(mercurySample);

        if (metadataRecord != null && metadataRecord.getValue() != null) {
            metadataRecord.setStringValue(newValue);
            log.info(String.format("Successfully updated metadata for sample '%s': '[%s, %s]", sampleKey,
                    metadataRecord.getKey(), metadataRecord.getValue()));
        } else {
            errors.put(sampleKey, metadataKey);
            String errorString = String.format(
                    "Could not find metadata matching original metadata '[%s, %s]' for sample '%s'. Please verify the oldValue and newValue are correct for this sample.",
                    metadataKey, oldValue, sampleKey);
            log.error(errorString);
        }
        return errors;
    }

    /**
     * test if metadata value matches original value. If there are errors it logs them
     */
    protected Map<String, Metadata.Key> validateOriginalValue(MercurySample mercurySample){
        return validateMetadataValue(mercurySample, oldValue);
    }

    /**
     * test if metadata value matches updated value. If there are errors it logs them.
     */
    protected Map<String, Metadata.Key> validateUpdatedValue(MercurySample mercurySample){
        return validateMetadataValue(mercurySample, newValue);
    }

    /**
     * test if metadata value matches expected value. If there are errors it logs them
     */
    private Map<String, Metadata.Key> validateMetadataValue(MercurySample mercurySample, String expected){
        Map<String, Metadata.Key> errors=new HashMap<>();
        for (Metadata metadata : mercurySample.getMetadata()) {
            if (metadata.getKey() == metadataKey) {
                if (!metadata.getValue().equals(expected)) {
                    String errorMessage =
                            String.format("Metadata value %s does not match expected value of %s for sample %s", metadata.getValue(),
                                    expected, mercurySample.getSampleKey());
                    log.error(errorMessage);
                    errors.put(sampleKey, metadataKey);
                }
            }
        }
        return errors;
    }

    private Metadata findMetadataRecord(MercurySample mercurySample) {
        for (Metadata metadata : mercurySample.getMetadata()) {
            if (metadata.getKey().equals(metadataKey)){
                return metadata;
            }
        }
        return null;
    }

    public String getSampleKey() {
        return sampleKey;
    }
}

