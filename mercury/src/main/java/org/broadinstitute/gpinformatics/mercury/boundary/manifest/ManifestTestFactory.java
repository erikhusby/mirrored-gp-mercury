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

    public static ManifestRecord buildManifestRecord(ManifestSession manifestSession, Map<Metadata.Key, String> metadataContents) {
        ManifestRecord manifestRecord = new ManifestRecord(buildMetadata(metadataContents));
        manifestRecord.setSession(manifestSession);
        return manifestRecord;
    }

    public static ManifestRecord buildManifestRecord(Map<Metadata.Key, String> metadataContents) {
        return buildManifestRecord(null, metadataContents);
    }
}
