package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import java.util.Collection;
import java.util.List;

/**
 * Client to get exported sample from BSP.
 */
public class BSPGetExportedSamplesFromAliquots extends BSPJerseyClient {

    class ExportedSamples {
        private String lsid;
        private String participantLsid;
        private String exportedLsid;
        private String destination;
        private String exportDate;
    }

    List<ExportedSamples> getExportedSamplesFromAliquots( Collection<String> sampleLsids,
            IsExported.ExternalSystem externalSystem) {
        return null;
    }
}
