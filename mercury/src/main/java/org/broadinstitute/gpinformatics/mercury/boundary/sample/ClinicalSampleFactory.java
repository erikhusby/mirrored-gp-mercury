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
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Static methods to simplify the creation of ClinicalResourceBean and related objects.
 */
public class ClinicalSampleFactory {

    public static final String SAMPLE_CONTAINS_NO_METADATA = "Sample contains no metadata.";
    public static final String SAMPLE_IS_NULL = "Sample is null.";
    public static final String EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED = "Empty list of samples not allowed.";

    /**
     * Convert a Sample's SampleData to Mercury Metadata.
     */
    public static Metadata[] toMercuryMetadata(Sample sample) {
        Metadata[] mercuryMetadata = new Metadata[sample.getSampleData().size()];
        List<SampleData> sampleData1 = sample.getSampleData();
        for (int i = 0; i < sampleData1.size(); i++) {
            SampleData sampleData = sampleData1.get(i);
            Metadata.Key metadataKey = Metadata.Key.valueOf(sampleData.getName());
            Metadata mercuryMetadataItem = new Metadata(metadataKey, sampleData.getValue());
            mercuryMetadata[i] = mercuryMetadataItem;
        }

        return mercuryMetadata;
    }

    /**
     * Convert a collection of Samples to ManifestRecords.
     */
    public static Collection<ManifestRecord> toManifestRecords(Collection<Sample> samples) {
        if (samples.isEmpty()) {
            throw new RuntimeException(EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED);
        }
        List<ManifestRecord> manifestRecords = new ArrayList<>();
        for (Sample sample : samples) {
            if (sample == null) {
                throw new RuntimeException(SAMPLE_IS_NULL);
            }
            if (CollectionUtils.isEmpty(sample.getSampleData())) {
                throw new RuntimeException(SAMPLE_CONTAINS_NO_METADATA);
            }
            manifestRecords.add(new ManifestRecord(toMercuryMetadata(sample)));
        }
        return manifestRecords;
    }
}
