package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Factory for Buick manifest test data.
 */
public class ManifestTestFactory {

    private static final int NUM_HEADER_ROWS = 1;

    public static Metadata[] buildMetadata(Map<Metadata.Key, String> metadataContents) {
        List<Metadata> metadataList = new ArrayList<>();

        for (Map.Entry<Metadata.Key, String> entry : metadataContents.entrySet()) {
            Metadata metadata = new Metadata(entry.getKey(), entry.getValue());
            metadataList.add(metadata);
        }
        return metadataList.toArray(new Metadata[metadataList.size()]);
    }

    public static ManifestSession buildManifestSession(String researchProjectKey, String sessionPrefix,
                                                       BspUser createdBy, int numberOfRecords,
                                                       ManifestRecord.Status defaultStatus) {
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(researchProjectKey);
        ManifestSession manifestSession = new ManifestSession(researchProject, sessionPrefix,
                createdBy);

        for (int i = 1; i <= numberOfRecords; i++) {
            ManifestRecord manifestRecord = buildManifestRecord(i);
            manifestRecord.setStatus(defaultStatus);
            manifestSession.addRecord(manifestRecord);
        }
        return manifestSession;
    }

    public static ManifestRecord buildManifestRecord(int recordNumber) {
        return buildManifestRecord(recordNumber, null);
    }

    public static ManifestRecord buildManifestRecord(int recordNumber, Map<Metadata.Key, String> initialData) {
        ManifestRecord manifestRecord = new ManifestRecord();

        for (Metadata.Key key : Metadata.Key.values()) {
            if (key.getCategory() == Metadata.Category.SAMPLE) {
                String value;
                if (initialData != null && initialData.containsKey(key)) {
                    value = initialData.get(key);
                } else {
                    value = key.name() + "_" + recordNumber;
                }
                Metadata metadata = new Metadata(key, value);
                manifestRecord.getMetadata().add(metadata);
            }
        }
        return manifestRecord;
    }

    public static void addRecord(ManifestSession session, ManifestRecord.ErrorStatus errorStatus,
                                 ManifestRecord.Status status, Map<Metadata.Key, String> initialData) {
        ManifestRecord record = buildManifestRecord(20, initialData);
        record.setStatus(status);
        session.addRecord(record);

        if (errorStatus != null) {
            session.addManifestEvent(new ManifestEvent(errorStatus,
                    errorStatus.formatMessage(Metadata.Key.SAMPLE_ID, record.getSampleId()), record));
        }
    }

}
