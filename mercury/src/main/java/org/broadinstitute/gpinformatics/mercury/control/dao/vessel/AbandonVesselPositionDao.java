package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVesselPosition_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
public class AbandonVesselPositionDao extends GenericDao{

    public AbandonVesselPosition findByIdentifier(Long vessel) {
        return findSingle(AbandonVesselPosition.class, AbandonVesselPosition_.abandonVesselPositionId, vessel);
    }
}
