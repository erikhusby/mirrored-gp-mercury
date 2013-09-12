package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.io.Serializable;
import java.util.Set;

/**
 *
 */
public interface BSPSampleReceiptService extends Serializable {

    public void receiveSamples(Set<String> sampleInfoMap, String username);
}
