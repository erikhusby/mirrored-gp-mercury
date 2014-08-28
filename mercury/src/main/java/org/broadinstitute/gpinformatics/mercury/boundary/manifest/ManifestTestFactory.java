package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Factory for Buick manifest test data.
 */
public class ManifestTestFactory {

    private static Metadata [] buildMetadata(Map<Metadata.Key, String> metadataContents) {
        List<Metadata> metadataList = new ArrayList<>();

        for (Map.Entry<Metadata.Key, String> entry : metadataContents.entrySet()) {
            Metadata metadata = new Metadata(entry.getKey(), entry.getValue());
            metadataList.add(metadata);
        }
        return metadataList.toArray(new Metadata[metadataList.size()]);
    }

    public static ManifestRecord buildManifestRecord(Map<Metadata.Key, String> metadataContents,
                                                     ManifestSession sessionIn) {
        return new ManifestRecord(sessionIn, buildMetadata(metadataContents));
    }

    public static ManifestRecord buildManifestRecord(ManifestRecord.ErrorStatus errorStatus,
                                                     Map<Metadata.Key, String> metadataContents,
                                                     ManifestSession sessionIn) {
        return new ManifestRecord(sessionIn, errorStatus, buildMetadata(metadataContents));
    }
}
