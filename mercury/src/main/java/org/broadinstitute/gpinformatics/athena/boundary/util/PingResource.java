package org.broadinstitute.gpinformatics.athena.boundary.util;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Restful webservice to
 * list the athena research project info.
 */
@Path("/util")
@Stateless
public class PingResource {

    @Inject
    private ResearchProjectDao rpDao;
    @Inject
    private ProductOrderDao pdoDao;
    @Inject
    private ProductDao pDao;

    /**
     * Ping service for use in app monitor
     * an attempy will be made to load a Product, PDO & RP
     * a 200 and the word "OK"
     *
     * @return String - "OK"
     */
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    @Path("/ping")
    public List<String> ping() {
        List<String> results = new ArrayList<String>();

         List<ResearchProject> rpList = rpDao.findAllResearchProjects();
        for (ResearchProject rp : rpList) {
            results.add(rp.getTitle() + "\n");
        }

        List<ProductOrder> pdoList = pdoDao.findAll();
        for (ProductOrder pdo : pdoList) {
            results.add(pdo.getTitle() +  "\n");
        }

        List<Product> pList = pDao.findProducts();
        for (Product p : pList) {
            results.add(p.getProductName() +  "\n");
        }

        return results;
    }
}
