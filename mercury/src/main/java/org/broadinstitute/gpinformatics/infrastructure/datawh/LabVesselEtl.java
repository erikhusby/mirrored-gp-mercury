package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

@Stateful
public class LabVesselEtl extends GenericEntityEtl<LabVessel, LabVessel> {

    @Inject
    public LabVesselEtl(LabVesselDao dao) {
        super(LabVessel.class, "lab_vessel", dao);
    }

    @Override
    Long entityId(LabVessel entity) {
        return entity.getLabVesselId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(LabVessel_.labVesselId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabVessel.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabVessel entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getLabVesselId(),
                format(entity.getLabel()),
                format(entity.getType().getName())
        );
    }
}
