/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simplifies the creation of Samples and ClinicalResourceBeans. Used for testing.
 */
public class ClinicalSampleTestFactory {

    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, boolean isFromSampleKit,
                                                                  int sampleCount) {
        return createClinicalResourceBean(userName, manifestName, researchProjectKey, isFromSampleKit,
                        getRandomTestSamples(sampleCount));
    }

    public static Collection<Sample> getRandomTestSamples(int count) {
        List<Sample> samples = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            samples.add(createSample(getRandomMetadataMap(i)));
        }
        return samples;
    }

    public static Map<Metadata.Key, String> getRandomMetadataMap(final int smNum) {
        return new HashMap<Metadata.Key, String>() {{
                put(Metadata.Key.SAMPLE_ID, "SM-" + smNum);
                put(Metadata.Key.PATIENT_ID, RandomStringUtils.randomAlphanumeric(8));
                put(Metadata.Key.PERCENT_TUMOR, "");
                put(Metadata.Key.MATERIAL_TYPE, null);
            }};
    }

    /**
     * Create a ClinicalResourceBean with specified values and a list of Metadata key-value pairs.
     */
    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, boolean isFromSampleKit,
                                                                  Map<Metadata.Key, String>... sampleMetadataList) {
        Set<Sample> sampleData = new HashSet<>();
        for (Map<Metadata.Key, String> sampleMetadata : sampleMetadataList) {
            sampleData.add(createSample(sampleMetadata));
        }

        return createClinicalResourceBean(userName, manifestName, researchProjectKey, isFromSampleKit, sampleData);
    }

    /**
     * Create a ClinicalResourceBean with specified values.
     */
    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, boolean isFromSampleKit,
                                                                  Collection<Sample> sampleData) {
        ClinicalResourceBean clinicalResourceBean = new ClinicalResourceBean();
        clinicalResourceBean.setManifestName(manifestName);
        clinicalResourceBean.setResearchProjectKey(researchProjectKey);
        clinicalResourceBean.setFromSampleKit(isFromSampleKit);
        clinicalResourceBean.setUsername(userName);
        clinicalResourceBean.getSamples().addAll(sampleData);
        return clinicalResourceBean;
    }

    /**
     * Create one Sample with specified Metadata.
     */
    public static Sample createSample(Map<Metadata.Key, String> metaDataPairs) {
        Sample sample = new Sample();

        for (Map.Entry<Metadata.Key, String> metaDataEntry : metaDataPairs.entrySet()) {
            SampleData metaDataItem = new SampleData();
            metaDataItem.setName(metaDataEntry.getKey().name());
            metaDataItem.setValue(metaDataEntry.getValue());
            sample.getSampleData().add(metaDataItem);
        }
        return sample;
    }
}
