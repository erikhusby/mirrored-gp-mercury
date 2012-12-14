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
     *
     * @param product
     * @param partNumber
     * @throws ApplicationValidationException
     */
    private void validateUniquePartNumber(Product product, String partNumber) throws ApplicationValidationException {
        Product existingProduct = productDao.findByPartNumber(partNumber);
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


    public void save(Product product, String partNumber) throws ApplicationValidationException {

        validateUniquePartNumber(product, partNumber);
        validateDateRangeOkay(product);

        // copy in the part number as the last thing we do before writing to db
        product.setPartNumber(partNumber);

        // if we are doing a create we will need to persist and flush, otherwise just falling off the end of this
        // @Stateful method will commit our transaction
        if (product.getProductId() == null) {
            try {
                productDao.persist(product);
                productDao.flush();
            }
            catch (RuntimeException e) {
                throw new ApplicationValidationException("Error creating Product", e);
            }
        }
    }

}
