package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
            workflowName = product.getWorkflowName();
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
            this.products = new ArrayList<ProductData>(products.size());
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
    public Products findOrders() {
        return new Products(productDao.findProductsForProductList());
    }
}