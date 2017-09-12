package org.broadinstitute.gpinformatics.mercury.control.dao.sample;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest_;

public class SampleKitRequestDao  extends GenericDao {

    public SampleKitRequest findByIdentifier(long sampleKitRequest) {
        return findSingle(SampleKitRequest.class, SampleKitRequest_.sampleKitRequestId, sampleKitRequest );
    }

    public SampleKitRequest findByOrganization(String organization) {
        return findSingle(SampleKitRequest.class, SampleKitRequest_.Organization, organization);
    }

    public SampleKitRequest findByEmail(String organization) {
        return findSingle(SampleKitRequest.class, SampleKitRequest_.email, organization);
    }
}
