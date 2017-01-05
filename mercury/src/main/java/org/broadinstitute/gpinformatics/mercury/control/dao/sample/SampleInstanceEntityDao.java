package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
public class SampleInstanceEntityDao extends GenericDao {

    public SampleInstanceEntity findByName(String sampleLibraryName) {
        return findSingle(SampleInstanceEntity.class, SampleInstanceEntity_.sampleLibraryName, sampleLibraryName );
    }

    public SampleInstanceEntity findByLabVessel(LabVessel labVessel) {
        return findSingle(SampleInstanceEntity.class, SampleInstanceEntity_.labVessel, labVessel );
    }

}
