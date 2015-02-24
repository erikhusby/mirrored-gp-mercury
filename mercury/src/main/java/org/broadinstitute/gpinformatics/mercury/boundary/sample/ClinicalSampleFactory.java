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

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static methods to simplify the creation of ClinicalResourceBean and related objects.
 */
public class ClinicalSampleFactory {
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

    /**
     * Create a ClinicalResourceBean with specified values and a list of Metadata key-value pairs.
     */
    public static ClinicalResourceBean createClinicalResourceBean(String userName, String manifestName,
                                                                  String researchProjectKey, boolean isFromSampleKit,
                                                                  Map<Metadata.Key, String> ... sampleMetadataList) {
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
     * Convert a Sample's SampleData to Mercury Metadata.
     */
    public static Metadata[] toMercuryMetadata(Sample sample) {
        List<Metadata> mercuryMetadata = new ArrayList<>(sample.getSampleData().size());
        for (SampleData sampleData : sample.getSampleData()) {
            Metadata.Key metadataKey = Metadata.Key.valueOf(sampleData.getName());
            Metadata mercuryMetadataItem = new Metadata(metadataKey, sampleData.getValue());
            mercuryMetadata.add(mercuryMetadataItem);
        }

        return mercuryMetadata.toArray(new Metadata[mercuryMetadata.size()]);
    }

    /**
     * Convert a collection of Samples to ManifestRecords.
     */
    public static Collection<ManifestRecord> toManifestRecords(Collection<Sample> samples) {
        if (samples.isEmpty()) {
            throw new InformaticsServiceException("Empty list of samples not allowed.");
        }
        List<ManifestRecord> manifestRecords = new ArrayList<>();
        for (Sample sample : samples) {
            if (sample == null) {
                throw new InformaticsServiceException("Sample is null.");
            }
            if (CollectionUtils.isEmpty(sample.getSampleData())) {
                throw new InformaticsServiceException("Sample contains no metadata.");
            }
            manifestRecords.add(new ManifestRecord(toMercuryMetadata(sample)));
        }
        return manifestRecords;
    }
}
