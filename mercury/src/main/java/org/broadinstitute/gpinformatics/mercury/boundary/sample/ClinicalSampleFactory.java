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

import org.apache.commons.lang3.StringUtils;
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
    public static List<Metadata> toMercuryMetadata(Sample sample) {
        List<Metadata> mercuryMetadata = new ArrayList<>(sample.getSampleData().size());
        for (SampleData sampleData : sample.getSampleData()) {
            if(StringUtils.isNotEmpty(sampleData.getValue())) {
                Metadata.Key metadataKey = Metadata.Key.valueOf(sampleData.getName());
                Metadata mercuryMetadataItem = new Metadata(metadataKey, sampleData.getValue());
                mercuryMetadata.add(mercuryMetadataItem);
            }
        }

        return mercuryMetadata;
    }

    /**
     * Convert a collection of Samples to ManifestRecords.
     */
    public static Collection<ManifestRecord> toManifestRecords(Collection<Sample> samples) {
        List<ManifestRecord> manifestRecords = new ArrayList<>(samples.size());
        for (Sample sample : samples) {
            List<Metadata> metadata = toMercuryMetadata(sample);
            manifestRecords.add(new ManifestRecord(metadata.toArray(new Metadata[metadata.size()])));
        }
        return manifestRecords;
    }

    /**
     * Test if provided sampleData includes all required fields.
     *
     * @return True if provided sampleData includes all required fields.<br/>
     *         False if any required fields are missing.
     */
    public static boolean hasRequiredMetadata(List<SampleData> sampleData) {
        List<String> includedDataFields=new ArrayList<>();
        for (SampleData data : sampleData) {
                if (Metadata.Key.isRequired(data.getName())) {
                    if (StringUtils.isNotBlank(data.getValue())) {
                        includedDataFields.add(data.getName());
                    }
                }
        }
        for (Metadata.Key key : Metadata.Key.getRequiredFields()) {
            if (!includedDataFields.contains(key.name())) {
                return false;
            }
        }
        return true;
    }

}
