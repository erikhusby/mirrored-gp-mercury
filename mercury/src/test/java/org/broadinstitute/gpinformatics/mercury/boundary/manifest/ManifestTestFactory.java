package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
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
                                                       ManifestRecord.Status defaultStatus, boolean fromSampleKit,
                                                       ManifestSessionEjb.AccessioningProcessType accessioningProcessType) {
        ResearchProject researchProject = ResearchProjectTestFactory.createTestResearchProject(researchProjectKey);
        ManifestSession manifestSession = new ManifestSession(researchProject, sessionPrefix,
                createdBy, fromSampleKit, accessioningProcessType);

        EnumSet<Metadata.Key> excludeKeys = EnumSet.noneOf(Metadata.Key.class);
        if(accessioningProcessType != ManifestSessionEjb.AccessioningProcessType.COVID) {
            excludeKeys.add(Metadata.Key.BROAD_2D_BARCODE);
            if(fromSampleKit){
                excludeKeys.add(Metadata.Key.SAMPLE_ID);
            }
        } else {
            excludeKeys.add(Metadata.Key.BROAD_SAMPLE_ID);
        }

        for (int i = 1; i <= numberOfRecords; i++) {
            ManifestRecord manifestRecord = buildManifestRecord(i, excludeKeys);
            manifestSession.addRecord(manifestRecord);
            manifestRecord.setStatus(defaultStatus);
        }
        return manifestSession;
    }

    public static ManifestRecord buildManifestRecord(int recordNumber, EnumSet<Metadata.Key> excludeKeys) {
        return buildManifestRecord(recordNumber, null, excludeKeys);
    }

    public static ManifestRecord buildManifestRecord(int recordNumber, Map<Metadata.Key, String> initialData,
                                                     EnumSet<Metadata.Key> excludeKeys) {
        ManifestRecord manifestRecord = new ManifestRecord();

        for (Metadata.Key key : Metadata.Key.values()) {
            if ((key.getCategory() == Metadata.Category.SAMPLE &&
                key != Metadata.Key.BROAD_2D_BARCODE)
                && (!excludeKeys.contains(key))
                    ) {
                Metadata metadata = null;
                if (initialData != null && initialData.containsKey(key)) {
                    metadata = new Metadata(key, initialData.get(key));
                } else {
                    switch (key.getDataType()) {
                    case STRING:
                        metadata = new Metadata(key, key.name() + "_" + recordNumber);
                        break;
                    case NUMBER:
                        metadata = new Metadata(key, new BigDecimal(recordNumber));
                        break;
                    case DATE:
                        metadata = new Metadata(key, new Date());
                        break;
                    }
                }

                manifestRecord.getMetadata().add(metadata);
            }
        }
        return manifestRecord;
    }

    public static void addRecord(ManifestSession session, ManifestRecord.ErrorStatus errorStatus,
                                 ManifestRecord.Status status, Map<Metadata.Key, String> initialData,
                                 EnumSet<Metadata.Key> excludeKeys) {
        ManifestRecord record = buildManifestRecord(20, initialData, excludeKeys);
        session.addRecord(record);
        record.setStatus(status);


        if (errorStatus != null) {
            session.addManifestEvent(new ManifestEvent(errorStatus,
                    errorStatus.formatMessage(Metadata.Key.SAMPLE_ID, record.getSampleId()), record));
        }
    }

}
