package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.presentation.products.WorkflowDiagrammer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;

import javax.annotation.Nonnull;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Restful webservice to list products.
 */

@Path("products")
@Stateful
@RequestScoped
public class ProductResource {
    private static final String WORKFLOW_DIAGRAM_IMAGE_URL = "products/workflowDiagrams/";

    @Inject
    private ProductDao productDao;

    @Inject
    private WorkflowDiagrammer diagrammer;

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

    /**
     * Invokes the WorkflowDiagrammer to make diagram files.
     *
     * @return the diagram file names that were made.
     */
    @GET
    @Path("workflowDiagrams")
    @Produces(MediaType.TEXT_PLAIN)
    public String makeWorkflowDiagrams() throws Exception {
        diagrammer.makeAllDiagramFiles();
        String filenames = StringUtils.join(WorkflowDiagrammer.listDiagramFiles(), "  ");
        return "Created diagram files: " + filenames;
    }

    @Nonnull
    public String createWorkflowImageUrl(ProductWorkflowDef productWorkflowDef, Date effectiveDate) {
        return WORKFLOW_DIAGRAM_IMAGE_URL +
               WorkflowDiagrammer.getWorkflowImageFileName(productWorkflowDef, effectiveDate);
    }

    /**
     * Returns a workflow diagram image, or null if no such diagram exists.
     */
    @GET
    @Path("workflowDiagrams/{filename}")
    @Produces("image/png")
    public byte[] getWorkflowDiagramImage(@PathParam("filename") String filename) throws Exception {
        File diagramFileDir = WorkflowDiagrammer.makeDiagramFileDir();
        // If diagramFile is not found in the directory, invoke the diagrammer to create all of the workflow diagrams.
        // Caching all of them will speed up the next viewing.  The cache is flushed when the app starts, so no
        // fear of caching an stale diagram.
        File diagramFile = new File(diagramFileDir, filename);
        if (!diagramFile.exists()) {
            diagrammer.makeAllDiagramFiles();
        }
        if (diagramFile.exists()) {
            InputStream stream = new BufferedInputStream(new FileInputStream(diagramFile));
            byte[] diagram = IOUtils.toByteArray(stream);
            IOUtils.closeQuietly(stream);
            return diagram;
        } else {
            return null;
        }
    }
}
