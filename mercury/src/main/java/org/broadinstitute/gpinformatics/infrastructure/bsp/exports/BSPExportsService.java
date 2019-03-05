package org.broadinstitute.gpinformatics.infrastructure.bsp.exports;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.Collection;

@Dependent
public class BSPExportsService implements Serializable {

    private static Log log = LogFactory.getLog(BSPExportsService.class);

    @Inject
    private BSPConfig bspConfig;

    /**
     * Return the Set of ExternalSystems to which this collection of LabVessels has been exported.
     */
    public IsExported.ExportResults findExportDestinations(@Nonnull Collection<LabVessel> labVessels) {
        if (CollectionUtils.isEmpty(labVessels)) {
            throw new InformaticsServiceException("Null or empty collection of LabVessels submitted for export check.");
        }

        // Copy the resource above with the query parameters added.
        String url = bspConfig.getUrl("rest/exports/isExported");
        WebTarget target = ClientBuilder.newClient().target(url);
        for (LabVessel labVessel : labVessels) {
            if (labVessel != null) {
                target = target.queryParam("barcode", labVessel.getLabel());
            }
        }

        return JaxRsUtils.getAndCheck(target.request(MediaType.APPLICATION_XML_TYPE), IsExported.ExportResults.class);
    }

    /**
     * Initiates an export to Mercury.
     * @param containerId CO-ID
     * @param userId user initiating export
     */
    public void export(String containerId, String userId) {
        WebTarget resource = ClientBuilder.newClient().target(bspConfig.getUrl("rest/exports"))
                .queryParam("containerId", containerId)
                .queryParam("userId", userId);
        resource.request().post(null);
    }
}
