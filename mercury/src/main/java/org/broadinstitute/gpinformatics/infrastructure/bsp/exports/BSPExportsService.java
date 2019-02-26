package org.broadinstitute.gpinformatics.infrastructure.bsp.exports;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Dependent
public class BSPExportsService implements Serializable {

    private static Log log = LogFactory.getLog(BSPExportsService.class);

    @Inject
    private BSPConfig bspConfig;

    private Client client = ClientBuilder.newClient();

    /**
     * Return the Set of ExternalSystems to which this collection of LabVessels has been exported.
     */
    public IsExported.ExportResults findExportDestinations(@Nonnull Collection<LabVessel> labVessels) {
        if (CollectionUtils.isEmpty(labVessels)) {
            throw new InformaticsServiceException("Null or empty collection of LabVessels submitted for export check.");
        }

        List<String> barcodes = new ArrayList<>();
        for (LabVessel labVessel : labVessels) {
            if (labVessel != null) {
                barcodes.add(labVessel.getLabel());
            }
        }

        // Copy the resource above with the query parameters added.
        String url = bspConfig.getUrl("rest/exports/isExported");
        WebTarget webTarget = client.target(url).queryParam("barcode", barcodes); // todo jmt

        return webTarget.request(MediaType.APPLICATION_XML_TYPE).get(IsExported.ExportResults.class);
    }

    /**
     * Initiates an export to Mercury.
     * @param containerId CO-ID
     * @param userId user initiating export
     */
    public void export(String containerId, String userId) {
        WebTarget resource = client.target(bspConfig.getUrl("rest/exports"));
        MultivaluedHashMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("containerId", containerId);
        formData.add("userId", userId);
        resource.request().post(Entity.form(formData));
    }
}
