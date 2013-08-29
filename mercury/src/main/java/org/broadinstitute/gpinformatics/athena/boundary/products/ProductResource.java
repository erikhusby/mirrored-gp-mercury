package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.presentation.products.WorkflowDiagramer;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Restful webservice to list products.
 */

@Path("products")
@Stateful
@RequestScoped
public class ProductResource {

    @Inject
    private ProductDao productDao;

    @Inject
    private WorkflowDiagramer diagramer;

    @XmlRootElement
    public static class ProductData {
        public final String name;
        public final String family;
        public final String partNumber;
        public final String workflowName;

        // Required by JAXB.
        ProductData() {
            name = null;
            family = null;
            partNumber = null;
            workflowName = null;
        }

        public ProductData(Product product) {
            name = product.getProductName();
            family = product.getProductFamily().getName();
            partNumber = product.getPartNumber();
            workflowName = product.getWorkflow().getWorkflowName();
        }
    }

    @XmlRootElement
    public static class Products {

        @XmlElement(name = "product")
        public final List<ProductData> products;

        public Products() {
            products = null;
        }

        public Products(List<Product> products) {
            this.products = new ArrayList<>(products.size());
            for (Product order : products) {
                this.products.add(new ProductData(order));
            }
        }
    }

    /**
     * Method to GET the list of products. All products are returned.
     *
     * @return The product orders that match
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Products findProducts() {
        return new Products(productDao.findProductsForProductList());
    }

    @GET
    @Path("workflowDiagrams")
    @Produces(MediaType.TEXT_PLAIN)
    public String makeWorkflowDiagrams() throws Exception {
        StringBuilder builder = new StringBuilder();
        String destDirName = diagramer.makeAllDiagramFiles();
        File destDir = new File(destDirName);
        for (File diagramFile : destDir.listFiles()) {
            builder.append("  ").append(diagramFile.getName());
        }
        return "Created diagrams: " + builder.toString() + "    ";
    }

    @GET
    @Path("workflowDiagrams/{filename}")
    @Produces("image/jpeg")
    public byte[] getImageRepresentation(@PathParam("filename") String filename) throws Exception {
        File diagramFile = new File(WorkflowDiagramer.DIAGRAM_DIRECTORY, filename);
        if (diagramFile.exists()) {
            InputStream stream = new BufferedInputStream(new FileInputStream(diagramFile));
            byte[] diagram = IOUtils.toByteArray(stream);
            return diagram;
        } else {
            return null;
        }
  }

}