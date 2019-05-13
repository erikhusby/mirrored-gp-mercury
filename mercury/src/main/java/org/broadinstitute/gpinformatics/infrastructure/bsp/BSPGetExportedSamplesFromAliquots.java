package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.mercury.BSPJaxRsClient;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJaxRsClientService;

import javax.enterprise.context.Dependent;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Client to get exported sample from BSP.
 */
@Dependent
public class BSPGetExportedSamplesFromAliquots extends BSPJaxRsClient {

    public static class ExportedSample {
        private String lsid;
        private String participantLsid;
        private String exportedLsid;
        private String destination;
        private String exportDate;

        ExportedSample(String lsid, String participantLsid, String exportedLsid, String destination, String exportDate) {
            this.lsid = lsid;
            this.participantLsid = participantLsid;
            this.exportedLsid = exportedLsid;
            this.destination = destination;
            this.exportDate = exportDate;
        }

        public String getLsid() {
            return lsid;
        }

        public String getParticipantLsid() {
            return participantLsid;
        }

        public String getExportedLsid() {
            return exportedLsid;
        }

        public String getDestination() {
            return destination;
        }

        public String getExportDate() {
            return exportDate;
        }
    }

    public List<ExportedSample> getExportedSamplesFromAliquots( Collection<String> sampleLsids,
            IsExported.ExternalSystem externalSystem) {
        String urlString = getUrl("sample/getexportedsamplesbyaliquot");
        MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
        params.addAll("sample_lsids", new ArrayList<>(sampleLsids));
        params.add("export_destination", externalSystem.name());
        final List<ExportedSample> exportedSamples = new ArrayList<>();
        post(urlString, params, ExtraTab.FALSE, new AbstractJaxRsClientService.PostCallback() {
            @Override
            public void callback(String[] bspData) {
                exportedSamples.add(new ExportedSample(bspData[0], bspData[1], bspData[2], bspData[3],
                        bspData[4]));
            }
        });

        return exportedSamples;
    }
}
