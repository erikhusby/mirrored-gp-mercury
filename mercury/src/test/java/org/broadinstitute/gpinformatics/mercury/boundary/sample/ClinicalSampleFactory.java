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

import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClinicalSampleFactory {
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
}
