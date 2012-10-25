package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * The workflow definition for a product, composed of processes
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDef {

    /** e.g. Exome Express */
    private String name;

    /** List of versions */
    List<ProductWorkflowDefVersion> productWorkflowDefVersions = new ArrayList<ProductWorkflowDefVersion>();

    /** Transient list of versions, in descending order of effective date */
    private transient ArrayList<ProductWorkflowDefVersion> workflowVersionsDescEffDate;

    public ProductWorkflowDef(String name) {
        this.name = name;
    }

    /** For JAXB */
    @SuppressWarnings("UnusedDeclaration")
    ProductWorkflowDef() {
    }

    public String getName() {
        return name;
    }

    public void addProductWorkflowDefVersion(ProductWorkflowDefVersion productWorkflowDefVersion) {
        this.productWorkflowDefVersions.add(productWorkflowDefVersion);
    }

    public List<String> validate(LabVessel labVessel, String nextEventTypeName) {
        List<String> errors = new ArrayList<String>();
        ProductWorkflowDefVersion effectiveWorkflowDef = getEffectiveVersion();

        ProductWorkflowDefVersion.LabEventNode labEventNode = effectiveWorkflowDef.findStepByEventType(nextEventTypeName);
        if(labEventNode == null) {
            errors.add("Failed to find " + nextEventTypeName + " in " + name + " version " + effectiveWorkflowDef.getVersion());
        } else {
            labEventNode.getPredecessors();
        }
        return errors;
    }

    public ProductWorkflowDefVersion getEffectiveVersion() {
        // Sort by descending effective date
        if (workflowVersionsDescEffDate == null) {
            workflowVersionsDescEffDate = new ArrayList<ProductWorkflowDefVersion>(productWorkflowDefVersions);
            Collections.sort(workflowVersionsDescEffDate, new Comparator<ProductWorkflowDefVersion>() {
                @Override
                public int compare(ProductWorkflowDefVersion o1, ProductWorkflowDefVersion o2) {
                    return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                }
            });
        }

        Date now = new Date();
        ProductWorkflowDefVersion mostRecentEffWorkflowVersion = null;
        for (ProductWorkflowDefVersion productWorkflowDefVersion : workflowVersionsDescEffDate) {
            if(productWorkflowDefVersion.getEffectiveDate().before(now)) {
                mostRecentEffWorkflowVersion = productWorkflowDefVersion;
                break;
            }
        }
        assert mostRecentEffWorkflowVersion != null;
        return mostRecentEffWorkflowVersion;
    }
}
