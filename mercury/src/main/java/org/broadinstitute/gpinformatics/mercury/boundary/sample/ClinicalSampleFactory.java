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
    /**
     * Convert a Sample's SampleData to Mercury Metadata.
     */
    public static Metadata[] toMercuryMetadata(Sample sample) {
        Metadata[] mercuryMetadata = new Metadata[sample.getSampleData().size()];
        for (int i = 0; i < sample.getSampleData().size(); i++) {
            SampleData sampleData = sample.getSampleData().get(i);
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
        List<ManifestRecord> manifestRecords = new ArrayList<>();
        for (Sample sample : samples) {
            manifestRecords.add(new ManifestRecord(toMercuryMetadata(sample)));
        }
        return manifestRecords;
    }
}
