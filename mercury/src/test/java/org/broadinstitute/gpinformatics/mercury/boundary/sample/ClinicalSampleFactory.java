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
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

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

    protected static String writeValue(Object bean) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(outputStream, bean);
        return outputStream.toString();
    }

    public static String serializeSamples(Collection<Sample> crspSamples) {
        String value = "";
        try {
            value = writeValue(crspSamples);
            return value;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    @Test(groups = TestGroups.DATABASE_FREE, enabled = true)
    public void testSerializeSamples() throws Exception {

        List<Sample> sampleList = Arrays.asList(
                createCrspSample(ImmutableMap.of(
                        Metadata.Key.PATIENT_ID, "004-002", Metadata.Key.SAMPLE_ID, "03101231193")),
                createCrspSample(ImmutableMap.of(
                        Metadata.Key.PATIENT_ID, "204-003", Metadata.Key.SAMPLE_ID, "23101231193"))
        );

        String serialized = ClinicalSampleFactory.serializeSamples(sampleList);
        assertThat(serialized, is(not(isEmptyString())));
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
