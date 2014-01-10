package org.broadinstitute.gpinformatics.infrastructure.bsp.exports;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BSPExportsService {

    private static Log log = LogFactory.getLog(BSPExportsService.class);

    @Inject
    private BSPConfig bspConfig;

    private Client client = Client.create();

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

        MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
        parameters.put("barcode", barcodes);
        // Copy the resource above with the query parameters added.
        String url = bspConfig.getUrl("rest/exports/isExported");
        WebResource resource = client.resource(url).queryParams(parameters);

        return resource.accept(MediaType.APPLICATION_XML_TYPE).get(IsExported.ExportResults.class);
    }
}
