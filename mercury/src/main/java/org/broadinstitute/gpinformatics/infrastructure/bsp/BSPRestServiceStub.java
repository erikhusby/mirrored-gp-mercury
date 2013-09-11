package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

@Stub
public class BSPRestServiceStub implements BSPRestService {
    @Override
    public Map<String,SampleInfo> fetchSampleDetailsByMatrixBarcodes(@Nonnull Collection<String> matrixBarcodes) {
        return null;
    }
}
