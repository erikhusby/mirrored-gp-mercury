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
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simplifies the creation of Samples and ClinicalResourceBeans. Used for testing.
 */
public class ClinicalSampleTestFactory {

    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, boolean isFromSampleKit,
                                                                  int sampleCount) {

        return ClinicalSampleFactory
                .createClinicalResourceBean(userName, manifestName, researchProjectKey, isFromSampleKit,
                        getRandomTestSamples(sampleCount));
    }

    public static Collection<Sample> getRandomTestSamples(int count) {
        List<Sample> samples = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            samples.add(ClinicalSampleFactory.createSample(ImmutableMap
                    .of(Metadata.Key.SAMPLE_ID, "SM-" + i, Metadata.Key.PATIENT_ID,
                            RandomStringUtils.randomAlphanumeric(8))));
        }
        return samples;
    }

}
