package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettaLimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryBeansType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantBeanType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/libraryquant")
public class LibraryQuantResource {

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private SystemRouter systemRouter;

    @Inject
    private BettaLimsConnector bettaLimsConnector;

    @Inject
    private BSPUserList bspUserList;

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/qpcrrun")
    public Response createQpcrRun(QpcrRunBean qpcrRunBean) {

        List<String> tubeBarcodes = new ArrayList<>();
        for (LibraryBeansType libraryBeans: qpcrRunBean.getLibraryBeans()) {
            tubeBarcodes.add(libraryBeans.getTubeBarcode());
        }
        switch (systemRouter.getSystemOfRecordForVesselBarcodes(tubeBarcodes)) {
            case MERCURY:
                BspUser bspUser = getBspUser(qpcrRunBean.getOperator());
                MessageCollection messageCollection = new MessageCollection();
                vesselEjb.createQpcrRunFromRunBean(qpcrRunBean, messageCollection, bspUser.getUserId());
                if (messageCollection.hasErrors()) {
                    String errors = StringUtils.join(messageCollection.getErrors(), ",");
                    return Response.serverError().entity(errors).build();
                } else {
                    return Response.ok().build();
                }
            case SQUID:
                return bettaLimsConnector.createQpcrRun(qpcrRunBean);
            default:
                throw new RuntimeException("Unable to route createQpcrRun for tubes: " + tubeBarcodes);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response createLibraryQuants(LibraryQuantRunBean libraryQuantRunBean) {
        List<String> tubeBarcodes = new ArrayList<>();
        for (LibraryQuantBeanType libraryBeans: libraryQuantRunBean.getLibraryQuantBeans()) {
            tubeBarcodes.add(libraryBeans.getTubeBarcode());
        }
        switch (systemRouter.getSystemOfRecordForVesselBarcodes(tubeBarcodes)) {
        case MERCURY:
            MessageCollection messageCollection = new MessageCollection();
            vesselEjb.createLibraryQuantsFromRunBean(libraryQuantRunBean, messageCollection);
            if (messageCollection.hasErrors()) {
                String errors = StringUtils.join(messageCollection.getErrors(), ",");
                return Response.serverError().entity(errors).build();
            } else {
                return Response.ok().build();
            }
        case SQUID:
            return bettaLimsConnector.createLibraryQuants(libraryQuantRunBean);
        default:
            throw new RuntimeException("Unable to route createQpcrRun for tubes: " + tubeBarcodes);
        }
    }

    private BspUser getBspUser(String operator) {
        BspUser bspUser = bspUserList.getByUsername(operator);
        if (bspUser == null) {
            throw new RuntimeException("Failed to find operator " + operator);
        }
        return bspUser;
    }

}
