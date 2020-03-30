package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryBeansType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantBeanType;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.VesselEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Stateful
@RequestScoped
@Path("/libraryquant")
public class LibraryQuantResource {

    @Inject
    private VesselEjb vesselEjb;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Path("/qpcrrun")
    public Response createQpcrRun(QpcrRunBean qpcrRunBean) {

        List<String> tubeBarcodes = new ArrayList<>();
        for (LibraryBeansType libraryBeans : qpcrRunBean.getLibraryBeans()) {
            tubeBarcodes.add(libraryBeans.getTubeBarcode());
        }
        BspUser bspUser = getBspUser(qpcrRunBean.getOperator());
        MessageCollection messageCollection = new MessageCollection();
        vesselEjb.createQpcrRunFromRunBean(qpcrRunBean, messageCollection, bspUser.getUserId());
        if (messageCollection.hasErrors()) {
            String errors = StringUtils.join(messageCollection.getErrors(), ",");
            return Response.serverError().entity(errors).build();
        } else {
            return Response.ok().build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response createLibraryQuants(LibraryQuantRunBean libraryQuantRunBean) {
        List<String> tubeBarcodes = new ArrayList<>();
        for (LibraryQuantBeanType libraryBeans: libraryQuantRunBean.getLibraryQuantBeans()) {
            tubeBarcodes.add(libraryBeans.getTubeBarcode());
        }
        MessageCollection messageCollection = new MessageCollection();
        BspUser bspUser = getBspUser(libraryQuantRunBean.getOperator());
        vesselEjb.createLibraryQuantsFromRunBean(libraryQuantRunBean, messageCollection, bspUser.getUserId());
        if (messageCollection.hasErrors()) {
            String errors = StringUtils.join(messageCollection.getErrors(), ",");
            return Response.serverError().entity(errors).build();
        } else {
            return Response.ok().build();
        }
    }

    private BspUser getBspUser(String operator) {
        BspUser bspUser = bspUserList.getByUsername(operator);
        if (bspUser == null) {
            throw new RuntimeException("Failed to find operator " + operator);
        }
        login(operator);
        return bspUser;
    }

    /**
     * Login, so the audit trail shows the user from the post.
     */
    private void login(String operator) {
        userBean.login(operator);
    }

}
