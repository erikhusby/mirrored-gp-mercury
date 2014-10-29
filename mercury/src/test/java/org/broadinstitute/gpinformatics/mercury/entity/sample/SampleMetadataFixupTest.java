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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Fixup test for repatienting
 */
@Test(groups = TestGroups.FIXUP)
public class SampleMetadataFixupTest extends Arquillian {
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
            fixUpErrors.putAll(fixupItem.updateMetadataForSamples(mercurySamples.get(0)));
        }
        String assertFailureReason =
                String.format("Error updating some or all samples: %s. Please consult server log for more information.",
                        fixUpErrors);
        assertThat(assertFailureReason, fixUpErrors, equalTo(Collections.EMPTY_MAP));
        mercurySampleDao
                .persist(new FixupCommentary("see https://gpinfojira.broadinstitute.org/jira/browse/GPLIM-3107"));
        mercurySampleDao.flush();
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

    public static Map<String, MetaDataFixupItem> mapOf(String sampleId, Metadata.Key metadataKey,
                                                       String oldValue, String newValue) {
        return ImmutableMap.of(sampleId, new MetaDataFixupItem(sampleId, metadataKey, oldValue, newValue));
    }

    public Map<String, Metadata.Key> updateMetadataForSamples(MercurySample mercurySample) {
        Map<String, Metadata.Key> errors = new HashMap<>();
        if (!mercurySample.getSampleKey().equals(sampleKey)) {
            throw new RuntimeException(
                    String.format("Expected sample %s but received %s", sampleKey, mercurySample.getSampleKey()));
        }

        Metadata metadataRecord = findMetadataRecord(mercurySample);

        if (metadataRecord!=null && metadataRecord.getValue()!=null) {
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

