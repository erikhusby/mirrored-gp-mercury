package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.context.Dependent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Client to get exported sample from BSP.
 */
@Dependent
public class BSPGetExportedSamplesFromAliquots extends BSPJerseyClient {

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
        String queryString = makeQueryString("sample_lsids", sampleLsids);
        queryString += "&export_destination=" + externalSystem;
        final List<ExportedSample> exportedSamples = new ArrayList<>();
        post(urlString, queryString, ExtraTab.FALSE, new AbstractJerseyClientService.PostCallback() {
            @Override
            public void callback(String[] bspData) {
                exportedSamples.add(new ExportedSample(bspData[0], bspData[1], bspData[2], bspData[3],
                        bspData[4]));
            }
        });

        return exportedSamples;
    }
}
