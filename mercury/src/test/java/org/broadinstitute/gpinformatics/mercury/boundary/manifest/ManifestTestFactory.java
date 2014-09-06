package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;


/**
 * Factory for Buick manifest test data.
 */
public class ManifestTestFactory {

    public static Metadata [] buildMetadata(Map<Metadata.Key, String> metadataContents) {
        List<Metadata> metadataList = new ArrayList<>();

        for (Map.Entry<Metadata.Key, String> entry : metadataContents.entrySet()) {
            Metadata metadata = new Metadata(entry.getKey(), entry.getValue());
            metadataList.add(metadata);
        }
        return metadataList.toArray(new Metadata[metadataList.size()]);
    }

    public static ManifestSession buildManifestSession(String researchProjectKey, String sessionPrefix,
                                                       BSPUserList.QADudeUser createdBy, int numberOfRecords) {
        return buildManifestSession(researchProjectKey, sessionPrefix, createdBy, numberOfRecords,
                ManifestRecord.Status.UPLOADED);
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

    public static ManifestRecord buildManifestRecord(int i, String sampleId) {
        ManifestRecord manifestRecord = new ManifestRecord();
        for (Metadata.Key key : Metadata.Key.values()) {
            String value;
            if(key == Metadata.Key.SAMPLE_ID && StringUtils.isNotBlank(sampleId)) {
                value = sampleId;
            } else {
                value = key.name() + "_" + i;
            }
            Metadata metadata = new Metadata(key, value);
            manifestRecord.getMetadata().add(metadata);
        }
        return manifestRecord;
    }

    public static void addExtraRecord(ManifestSession session, String sampleId, ManifestRecord.ErrorStatus targetStatus,
                                ManifestRecord.Status status) {
        ManifestRecord dupeRecord = buildManifestRecord(20, sampleId);
        dupeRecord.setStatus(status);
        session.addRecord(dupeRecord);
        Optional<ManifestRecord.ErrorStatus> possibleStatus = Optional.of(targetStatus);
        if(possibleStatus.isPresent()) {
            session.addManifestEvent(new ManifestEvent(getSeverity(possibleStatus.get()),
                    possibleStatus.get().formatMessage(Metadata.Key.SAMPLE_ID.name(), sampleId), dupeRecord));
        }
    }

    public static ManifestEvent.Severity getSeverity(ManifestRecord.ErrorStatus status) {
        EnumSet<ManifestRecord.ErrorStatus> quarantinedSet = EnumSet.of(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID,
                ManifestRecord.ErrorStatus.NOT_READY_FOR_TUBE_TRANSFER);
        return (quarantinedSet.contains(status))?ManifestEvent.Severity.QUARANTINED: ManifestEvent.Severity.ERROR;
    }

    public enum CreationType{UPLOAD, FACTORY}
}
