package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

/**
 * The workflow definition for a product, composed of processes
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDef {

    /** e.g. Exome Express */
    private String name;
    List<ProductWorkflowDefVersion> productWorkflowDefVersions = new ArrayList<ProductWorkflowDefVersion>();

    public ProductWorkflowDef(String name) {
        this.name = name;
    }

    /** For JAXB */
    ProductWorkflowDef() {
    }

    public String getName() {
        return name;
    }

    public void addProductWorkflowDefVersion(ProductWorkflowDefVersion productWorkflowDefVersion) {
        this.productWorkflowDefVersions.add(productWorkflowDefVersion);
    }
}
