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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simplifies the creation of Samples and ClinicalResourceBeans. Used for testing.
 */
public class ClinicalSampleFactory {
    public static Collection<Sample> getRandomTestSamples(int count) {
        List<Sample> samples = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            samples.add(ClinicalSampleFactory.createCrspSample(ImmutableMap
                    .of(Metadata.Key.SAMPLE_ID, "SM-" + i, Metadata.Key.PATIENT_ID,
                            RandomStringUtils.randomAlphanumeric(8))));
        }
        return samples;
    }

    /**
     * Create one Sample with specified Metadata.
     */
    public static Sample createCrspSample(Map<Metadata.Key, String> metaDataPairs) {
        Sample crspSample = new Sample();

        for (Map.Entry<Metadata.Key, String> metaDataEntry : metaDataPairs.entrySet()) {
            SampleData metaDataItem = new SampleData();
            metaDataItem.setName(metaDataEntry.getKey().getDisplayName());
            metaDataItem.setValue(metaDataEntry.getValue());
            crspSample.getSampleData().add(metaDataItem);
        }
        return crspSample;
    }

    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, Boolean isFromSampleKit,
                                                                  Map<Metadata.Key, String> ... sampleMetadataList) {
        ClinicalResourceBean clinicalResourceBean = new ClinicalResourceBean();
        clinicalResourceBean.setManifestName(manifestName);
        clinicalResourceBean.setResearchProjectKey(researchProjectKey);
        clinicalResourceBean.setFromSampleKit(isFromSampleKit);
        clinicalResourceBean.setUsername(userName);
        Set<Sample> sampleData = new HashSet<>();

        for (Map<Metadata.Key, String> sampleMetadata : sampleMetadataList) {
            sampleData.add(createCrspSample(sampleMetadata));
        }

        return createClinicalResourceBean(userName, manifestName, researchProjectKey, isFromSampleKit, sampleData);
    }

    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, Boolean isFromSampleKit,
                                                                  Collection<Sample> sampleData) {
        ClinicalResourceBean clinicalResourceBean = new ClinicalResourceBean();
        clinicalResourceBean.setManifestName(manifestName);
        clinicalResourceBean.setResearchProjectKey(researchProjectKey);
        clinicalResourceBean.setFromSampleKit(isFromSampleKit);
        clinicalResourceBean.setUsername(userName);
        clinicalResourceBean.getSamples().addAll(sampleData);
        return clinicalResourceBean;
    }

    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, boolean isFromSampleKit,
                                                                  int sampleCount) {

        return createClinicalResourceBean(userName, manifestName, researchProjectKey, isFromSampleKit,
                getRandomTestSamples(sampleCount));
    }
}
