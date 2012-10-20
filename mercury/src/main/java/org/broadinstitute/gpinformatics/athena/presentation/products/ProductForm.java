package org.broadinstitute.gpinformatics.athena.presentation.products;

import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@RequestScoped
public class ProductForm extends AbstractJsfBean {

    @Inject
    private ProductDetail detail;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private FacesContext facesContext;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;
    public static final Integer NULL_CYCLE_TIME = 0;

    public void initForm() {
        // Only initialize the form on postback. Otherwise, we'll leave the form as the user submitted it.
        if (!facesContext.isPostback()) {
            detail.initEmptyProduct();
        }
    }

    public String create() {

        Product product =  new Product(detail.getProductName(), getProductFamilyById(detail.getProductFamilyId()), detail.getDescription(),
                detail.getPartNumber(), detail.getAvailabilityDate(), detail.getDiscontinuedDate(),
                convertCycleTimeHoursToSeconds(detail.getExpectedCycleTimeHours()),
                convertCycleTimeHoursToSeconds(detail.getGuaranteedCycleTimeHours()),
                detail.getSamplesPerWeek(), detail.getInputRequirements(), detail.getDeliverables(), DEFAULT_TOP_LEVEL,
                DEFAULT_WORKFLOW_NAME);

        //TODO hmc under construction

        productDao.persist(product);

        //TODO hmc add more info in the details param
        addInfoMessage("Product created.", "Product " + product.getPartNumber() + " has been created.");
        return redirect("list");
    }

    private ProductFamily getProductFamilyById(Long productFamilyId) {
        return productFamilyDao.find(productFamilyId);
    }

    // TODO under construction not working nor tested.
    public String edit() {
        throw new RuntimeException("Not yet Implemented");
//        productDao.getEntityManager().merge(detail.getProduct());
//        return redirect("list");
    }

    /**
     * Converts cycle times from hours to seconds.
     * @param cycleTimeHours
     * @return the number of seconds.
     */
    private int convertCycleTimeHoursToSeconds(Integer cycleTimeHours) {
        return ( cycleTimeHours == null ? 0 : cycleTimeHours * ProductDetail.ONE_HOUR_IN_SECONDS);
    }



}
