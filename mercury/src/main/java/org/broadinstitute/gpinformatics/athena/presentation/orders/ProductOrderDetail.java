package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.BillableItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * Class for displaying details about a product order.
 */
@Named
@RequestScoped
public class ProductOrderDetail {
    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    ResearchProjectDao researchProjectDao;

    /** Key used to look up this product order. */
    private String productOrderKey;

    /** If non null, the default research project for a new product order */
    private String researchProjectKey;

    /** The product order we're currently displaying */
    private ProductOrder productOrder;

    // Dummy objects until we have DB backed objects.
    public static ResearchProject createDummyResearchProject() {
        ResearchProject researchProject = new ResearchProject(1111L, "MyResearchProject", "To study stuff.", ResearchProject.IRB_ENGAGED);

        researchProject.addFunding(new ResearchProjectFunding(researchProject, "TheGrant"));
        researchProject.addFunding(new ResearchProjectFunding(researchProject, "ThePO"));

        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.BROAD, "irb123"));
        researchProject.addIrbNumber(new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.OTHER, "irb456"));

        researchProject.addPerson(RoleType.SCIENTIST, 111L);
        researchProject.addPerson(RoleType.SCIENTIST, 222L);
        return researchProject;
    }

    public static ProductOrder createDummyProductOrder() {
        PriceItem priceItem = new PriceItem(PriceItem.Platform.GP, PriceItem.Category.EXOME_SEQUENCING_ANALYSIS,
                                    PriceItem.Name.EXOME_EXPRESS, "testQuoteId");
        Product dummyProduct = new Product("productName", new ProductFamily("ProductFamily"), "description",
                    "partNumber", new Date(), new Date(), 12345678, 123456, 100, "inputRequirements", "deliverables",
                    true, "workflowName");

        dummyProduct.addPriceItem(priceItem);
        ProductOrder order = new ProductOrder("title",
                new ArrayList<ProductOrderSample>(), "quote", dummyProduct,
                createDummyResearchProject());

        ProductOrderSample sample = new ProductOrderSample("SM-1234");
        sample.addBillableItem(new BillableItem(priceItem, new BigDecimal("1")));
        order.addSample(sample);

        return order;
    }

    @PostConstruct
    public void initEmpty() {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        // FIXME: need default constructor to create empty product order.
        productOrder = createDummyProductOrder();
        //productOrder = new ProductOrder();
    }

    public void load() {
        if ((productOrder == null) && !StringUtils.isBlank(productOrderKey)) {
            productOrder = productOrderDao.findByBusinessKey(productOrderKey);
        }
    }

    public String getProductOrderKey() {
        return productOrderKey;
    }

    public void setProductOrderKey(String productOrderKey) {
        this.productOrderKey = productOrderKey;
    }

    public String getResearchProjectKey() {
        return researchProjectKey;
    }

    public void setResearchProjectKey(String researchProjectKey) {
        this.researchProjectKey = researchProjectKey;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }
}
