package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/vessel")
@Stateful
@RequestScoped
public class VesselResource {

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @Path("/registerTubes")
    @Produces(MediaType.APPLICATION_XML)
    @GET
    public RegisterTubesResponseBean registerTubes(@QueryParam("barcodes") @Nonnull List<String> barcodes) {
        Map<String, SampleInfo> sampleInfoMap = bspSampleDataFetcher.fetchSampleDetailsByMatrixBarcodes(barcodes);

        RegisterTubesResponseBean responseBean = new RegisterTubesResponseBean();

        boolean error = false;

        for (Map.Entry<String, SampleInfo> entry : sampleInfoMap.entrySet()) {
            String barcode = entry.getKey();
            SampleInfo sampleInfo = entry.getValue();
            String well = null;
            String sampleId = null;
            if (sampleInfo != null) {
                well = sampleInfo.getWellPosition();
                sampleId = sampleInfo.getSampleId();
            } else {
                // Keep going even if error is true, we just won't do the registration.  We still want to return
                // results to the caller (an AE script) so it can show the user what's wrong.
                error = true;
            }

            if (!error) {
                // do registration
            }

            RegisterTubeBean tubeBean = new RegisterTubeBean(barcode, well, sampleId);
            responseBean.getRegisterTubeBeans().add(tubeBean);
        }

        return responseBean;
    }
}
