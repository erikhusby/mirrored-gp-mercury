package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import com.sun.tools.javac.jvm.Gen;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

@Stateful
@RequestScoped
public class SampleInstanceDao extends GenericDao {

    public SampleInstance findByName(String sampleLibraryName) {
        return findSingle(SampleInstance.class, SampleInstance_.sampleLibraryName, sampleLibraryName );
    }

}
