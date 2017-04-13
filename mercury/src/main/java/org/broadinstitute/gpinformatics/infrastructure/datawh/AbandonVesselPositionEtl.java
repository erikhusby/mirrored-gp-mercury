package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVesselPosition_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

/**
 * ETL AbandonVesselPosition entity into denormalized DW table ABANDON_VESSEL
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class AbandonVesselPositionEtl extends GenericEntityEtl<AbandonVesselPosition, AbandonVesselPosition> {

    public AbandonVesselPositionEtl(){}

    @Inject
    public AbandonVesselPositionEtl(LabVesselDao dao) {
        super(AbandonVesselPosition.class, "abandon_vessel_position", "abandon_vessel_position_aud"
                , "abandon_vessel_position_id", dao);
    }

    @Override
    Long entityId(AbandonVesselPosition entity) {
        return entity.getAbandonVesselPositionId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(AbandonVesselPosition_.abandonVesselPositionId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(AbandonVesselPosition.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, AbandonVesselPosition entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getAbandonVesselPositionId(),
                "AbandonVesselPosition",
                format(entity.getLabVessel() == null?null:entity.getLabVessel().getLabVessel().getLabVesselId()),
                format(entity.getPosition()),
                format(entity.getReason()==null?"":entity.getReason().toString()),
                format(entity.getAbandonedOn())
        );
    }

}
