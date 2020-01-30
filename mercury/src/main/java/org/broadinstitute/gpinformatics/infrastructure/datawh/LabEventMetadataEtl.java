package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class LabEventMetadataEtl extends GenericEntityEtl<LabEventMetadata, LabEventMetadata> {

    public LabEventMetadataEtl() {
    }

    @Inject
    public LabEventMetadataEtl(LabEventDao dao) {
        super(LabEventMetadata.class, "le_metadata", "le_metadata_aud", "lab_event_metadata_id", dao);
    }

    @Override
    Long entityId(LabEventMetadata entity) {
        return entity.getLabEventMetadataId();
    }

    @Override
    Path rootId(Root<LabEventMetadata> root) {
        return root.get(LabEventMetadata_.labEventMetadataId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabEventMetadata.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabEventMetadata entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabEventMetadata eventMetadata) {
        if (eventMetadata == null) {
            return Collections.emptyList();
        }
        List<String> dataRecords = new ArrayList<>();
        if (eventMetadata.getLabEvents() != null) {
            if (isDelete) {
                dataRecords.add(genericRecord(etlDateStr, isDelete,
                        eventMetadata.getLabEventMetadataId()));
            } else {
                // Code for persistence config metadata to event many to many, reality is 1:1
                for (LabEvent event : eventMetadata.getLabEvents()) {
                    dataRecords.add(genericRecord(etlDateStr, isDelete,
                            eventMetadata.getLabEventMetadataId(),
                            format(event.getLabEventId()),
                            format(eventMetadata.getLabEventMetadataType().name()),
                            format(eventMetadata.getValue())
                    ));
                }
            }
        }
        return dataRecords;
    }
}
