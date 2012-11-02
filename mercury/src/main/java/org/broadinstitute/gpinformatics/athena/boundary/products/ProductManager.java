package org.broadinstitute.gpinformatics.athena.boundary.products;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.projects.ApplicationValidationException;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;


/**
 * Boundary bean for Projects
 */
@Stateful
@RequestScoped
public class ProductManager {

    @Inject
    private Log log;

    @Inject
    private ProductDao productDao;

    /**
     * Make sure part number is not duplicated
     *
     * @param product
     * @throws ApplicationValidationException
     */
    private void validateUniquePartNumber(Product product) throws ApplicationValidationException {
        Product existingProduct = productDao.findByPartNumber(product.getPartNumber());
        if (existingProduct != null && ! existingProduct.getProductId().equals(product.getProductId())) {
            throw new ApplicationValidationException("Part number '" + product.getPartNumber() + "' is already in use");
        }
    }

    /**
     * Sanity check dates
     * @return
     */
    private void validateDateRangeOkay(Product product) throws ApplicationValidationException {

        if ((product.getAvailabilityDate() != null ) &&
                (product.getDiscontinuedDate() != null ) &&
                (product.getAvailabilityDate().after(product.getDiscontinuedDate()))) {
            throw new ApplicationValidationException("Availability date must precede discontinued date.");
        }
    }



    public void create(Product product) throws ApplicationValidationException {

        validateUniquePartNumber(product);
        validateDateRangeOkay(product);

        try {
            productDao.persist(product);
            productDao.flush();
        }
        catch (RuntimeException e) {
            throw new ApplicationValidationException("Error creating Product", e);
        }

    }


    public void edit(Product product) throws ApplicationValidationException {
        validateUniquePartNumber(product);
        validateDateRangeOkay(product);
    }


}
