package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel_;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.Collection;

/**
 * ETL AbandonVessel entity into denormalized DW table ABANDON_VESSEL
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class AbandonVesselEtl extends GenericEntityEtl<AbandonVessel, AbandonVessel> {

    public AbandonVesselEtl(){}

    @Inject
    public AbandonVesselEtl(LabVesselDao dao) {
        super(AbandonVessel.class, "abandon_vessel", "abandon_vessel_aud"
                , "abandon_vessel_id", dao);
    }

    @Override
    Long entityId(AbandonVessel entity) {
        return entity.getAbandonVesselId();
    }

    @Override
    Path rootId(Root root) {
        return root.get(AbandonVessel_.abandonVesselId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(AbandonVessel.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, AbandonVessel entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getAbandonVesselId(),
                format(entity.getLabVessel().getLabVesselId()),
                format(entity.getReason()==null?"":entity.getReason().toString()),
                format(entity.getAbandonedOn()),
                format(entity.getVesselPosition()==null?"":entity.getVesselPosition().name())
        );
    }
}
