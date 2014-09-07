package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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
            ManifestRecord manifestRecord = buildManifestRecord((int) i);
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

    public static void addExtraRecord(ManifestSession session, Map<Metadata.Key, String> initalData,
                                      ManifestRecord.ErrorStatus errorStatus,
                                      ManifestRecord.Status status) {
        ManifestRecord dupeRecord = buildManifestRecord(20, initalData);
        dupeRecord.setStatus(status);
        session.addRecord(dupeRecord);

        if (errorStatus != null) {
            session.addManifestEvent(new ManifestEvent(errorStatus.getSeverity(),
                    errorStatus.formatMessage(Metadata.Key.SAMPLE_ID.name(), dupeRecord.getSampleId()), dupeRecord));
        }
    }

    public enum CreationType {UPLOAD, FACTORY}
}
