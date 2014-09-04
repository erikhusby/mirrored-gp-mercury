package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
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
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(researchProjectKey);
        ManifestSession manifestSession = new ManifestSession(researchProject, sessionPrefix,
                createdBy);

        for (int i = 1; i <= numberOfRecords; i++) {
            ManifestRecord manifestRecord = new ManifestRecord();
            for (Metadata.Key key : Metadata.Key.values()) {
                Metadata metadata = new Metadata(key, key.name() + "_" + i);
                manifestRecord.getMetadata().add(metadata);
            }
            manifestSession.addRecord(manifestRecord);
        }
        return manifestSession;
    }
}                                                                   `
