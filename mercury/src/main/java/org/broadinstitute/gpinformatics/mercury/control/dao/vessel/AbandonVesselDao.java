package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
public class AbandonVesselDao extends GenericDao{

    public AbandonVessel findByIdentifier(Long vessel) {
        return findSingle(AbandonVessel.class, AbandonVessel_.abandonedVesselsId, vessel);
    }
}