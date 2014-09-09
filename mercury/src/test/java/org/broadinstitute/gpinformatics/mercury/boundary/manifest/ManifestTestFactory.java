package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
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

    public static Metadata[] buildMetadata(Map<Metadata.Key, String> metadataContents) {
        List<Metadata> metadataList = new ArrayList<>();

        for (Map.Entry<Metadata.Key, String> entry : metadataContents.entrySet()) {
            Metadata metadata = new Metadata(entry.getKey(), entry.getValue());
            metadataList.add(metadata);
        }
        return metadataList.toArray(new Metadata[metadataList.size()]);
    }

    public static ManifestSession buildManifestSession(String researchProjectKey, String sessionPrefix,
                                                       BSPUserList.QADudeUser createdBy, int numberOfRecords,
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

    public static ManifestRecord buildManifestRecord(int i) {
        return buildManifestRecord(i, null);
    }

    public static ManifestRecord buildManifestRecord(int i, Map<Metadata.Key, String> initialData) {
        ManifestRecord manifestRecord;

        manifestRecord = new ManifestRecord();

        for (Metadata.Key key : Metadata.Key.values()) {
            String value;
            if(initialData != null && initialData.containsKey(key)) {
                value=initialData.get(key);
            } else {
                value = key.name() + "_" + i;
            }
            Metadata metadata = new Metadata(key, value);
            manifestRecord.getMetadata().add(metadata);
        }
        return manifestRecord;
    }

    private static void addRecord(ManifestSession session, ManifestRecord.ErrorStatus errorStatus,
                                  ManifestRecord.Status status, Map<Metadata.Key, String> initialData) {
        ManifestRecord record = buildManifestRecord(20, initialData);
        record.setStatus(status);
        session.addRecord(record);

        if (errorStatus != null) {
            session.addManifestEvent(new ManifestEvent(errorStatus.getSeverity(),
                    errorStatus.formatMessage(Metadata.Key.SAMPLE_ID.name(), record.getSampleId()), record));
        }
    }

    public static void addRecord(ManifestSession manifestSession, ManifestRecord.ErrorStatus errorStatus,
                                 ManifestRecord.Status status) {
        addRecord(manifestSession, errorStatus, status, ImmutableMap.<Metadata.Key, String>of());
    }

    public static void addRecord(ManifestSession manifestSession, ManifestRecord.ErrorStatus errorStatus,
                                 ManifestRecord.Status status, Metadata.Key key, String value) {
        addRecord(manifestSession, errorStatus, status, ImmutableMap.of(key, value));
    }

    public static void addRecord(ManifestSession manifestSession,
                                 ManifestRecord.ErrorStatus errorStatus, ManifestRecord.Status status,
                                 Metadata.Key key1, String value1,
                                 Metadata.Key key2, String value2,
                                 Metadata.Key key3, String value3) {
        addRecord(manifestSession, errorStatus, status, ImmutableMap.of(key1, value1, key2, value2, key3, value3));
    }
}
