package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductComparator;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;

@Named
@RequestScoped
public class ProductForm extends AbstractJsfBean  implements Serializable {

    @Inject
    private ProductDao productDao;
    @Inject
    private ProductFamilyDao productFamilyDao;
    @Inject
    private FacesContext facesContext;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    public static final int ONE_HOUR_IN_SECONDS = 3600;
    private Product product;

    private List<Product> addOns;


    public void initForm() {
        if (!facesContext.isPostback()) {
            if  ((product.getPartNumber() != null) && !StringUtils.isBlank(product.getPartNumber())) {
                product = productDao.findByBusinessKey(product.getPartNumber());
                addOns.addAll( product.getAddOns() );
            }
        }
    }

    public void initEmptyProduct() {
        product = new Product(null, null, null, null, null, null,
                null, null, null, null, null, DEFAULT_TOP_LEVEL, DEFAULT_WORKFLOW_NAME);
        addOns = new ArrayList<Product>();
    }

    public List<ProductFamily> getProductFamilies() {
        return  productFamilyDao.findAll();
    }

    public String save() {
        if (getProduct().getProductId() == null ) {
            return create();
        } else {
            return edit();
        }
    }

    public String create() {
        try {
            for ( Product aProduct : addOns ) {
                product.addAddOn( aProduct );
            }
            productDao.persist(product);
            addInfoMessage("Product created.", "Product " + product.getPartNumber() + " has been created.");
        } catch (Exception e ) {
            String errorMessage = MessageFormat.format("Unknown exception occurred while persisting Product.", null);
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The Product Part-Number ''{0}'' is not unique.", product.getPartNumber());
            }
            addErrorMessage("Product not Created.", errorMessage, errorMessage + ": " + e);
            return "create";
        }
        return redirect("list");
    }

    public String edit() {
        try {
            for ( Product aProduct : addOns ) {
                product.addAddOn( aProduct );
            }
            productDao.getEntityManager().merge(getProduct());
            addInfoMessage("Product detail updated.", "Product " + getProduct().getPartNumber() + " has been updated.");
        } catch (Exception e ) {
            String errorMessage = MessageFormat.format("Unknown exception occurred while persisting Product.", e);
            if (GenericDao.IsConstraintViolationException(e)) {
                errorMessage = MessageFormat.format("The Product Part-Number ''{0}'' is not unique.", product.getPartNumber());
            }
            addErrorMessage("Product not updated.", errorMessage, errorMessage + ": " + e);
            return "create";
        }
        return redirect("list");
    }

    public Product getProduct() {
        return product;
    }
    public void setProduct(final Product product) {
        this.product = product;
    }

    public Integer getExpectedCycleTimeHours() {
        return convertCycleTimeSecondsToHours (product.getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeHours(final Integer expectedCycleTimeHours) {
        product.setExpectedCycleTimeSeconds( convertCycleTimeHoursToSeconds( expectedCycleTimeHours ) );
    }

    public Integer getGuaranteedCycleTimeHours() {
        return convertCycleTimeSecondsToHours (product.getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeHours(final Integer guaranteedCycleTimeHours) {
        product.setGuaranteedCycleTimeSeconds(convertCycleTimeHoursToSeconds(guaranteedCycleTimeHours));
    }


    public List<Product> getAddOns() {
        if (product == null) {
            return new ArrayList<Product>();
        }
        ArrayList<Product> addOns = new ArrayList<Product>(product.getAddOns());
        Collections.sort(addOns, new ProductComparator());
        return addOns;
    }

    public void setAddOns(final List<Product> addOns) {
        this.addOns = addOns;
    }

    /**
     * Converts cycle times from hours to seconds.
     * @param cycleTimeHours
     * @return the number of seconds.
     */
    public static Integer convertCycleTimeHoursToSeconds(Integer cycleTimeHours) {
        Integer cycleTimeSeconds = null;
        if ( cycleTimeHours != null ) {
            cycleTimeSeconds = ( cycleTimeHours == null ? 0 : cycleTimeHours.intValue() * ONE_HOUR_IN_SECONDS);
        }
        return cycleTimeSeconds;
    }

    /**
     * Converts cycle times from seconds to hours.
     * This method rounds down to the nearest hour
     * @param cycleTimeSeconds
     * @return the number of hours.
     */
    public static Integer convertCycleTimeSecondsToHours(Integer cycleTimeSeconds) {
        Integer cycleTimeHours = null;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_HOUR_IN_SECONDS ) {
            cycleTimeHours =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_HOUR_IN_SECONDS)) / ONE_HOUR_IN_SECONDS;
        }
        return cycleTimeHours;
    }



}
