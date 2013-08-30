package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.athena.presentation.products.WorkflowDiagramer;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The workflow definition for a product, composed of processes
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProductWorkflowDef implements Serializable {

    private static final long serialVersionUID = 20130101L;

    /** e.g. Exome Express */
    private String name;

    /** List of versions */
    private List<ProductWorkflowDefVersion> productWorkflowDefVersions = new ArrayList<>();

    /** Transient list of versions, in descending order of effective date */
    private transient ArrayList<ProductWorkflowDefVersion> workflowVersionsDescEffDate;
    private transient Map<String, ProductWorkflowDefVersion> productDefVersionsByVersion =
            new HashMap<>();

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
        this.productDefVersionsByVersion.put ( productWorkflowDefVersion.getVersion (), productWorkflowDefVersion );
    }

    /** Returns a list of all product defs sorted by decreasing effective date. */
    public List<ProductWorkflowDefVersion> getWorkflowVersionsDescEffDate() {
        if (workflowVersionsDescEffDate == null) {
            workflowVersionsDescEffDate = new ArrayList<>(productWorkflowDefVersions);
            Collections.sort(workflowVersionsDescEffDate, new Comparator<ProductWorkflowDefVersion>() {
                @Override
                public int compare(ProductWorkflowDefVersion o1, ProductWorkflowDefVersion o2) {
                    return o2.getEffectiveDate().compareTo(o1.getEffectiveDate());
                }
            });
        }
        return workflowVersionsDescEffDate;
    }

    public ProductWorkflowDefVersion getEffectiveVersion() {
        return getEffectiveVersion(new Date());
    }

    public ProductWorkflowDefVersion getEffectiveVersion(Date eventDate) {
        ProductWorkflowDefVersion mostRecentEffWorkflowVersion = null;
        for (ProductWorkflowDefVersion productWorkflowDefVersion : getWorkflowVersionsDescEffDate()) {
            // Should select workflow when effectiveDate <= eventDate.
            if (!productWorkflowDefVersion.getEffectiveDate().after(eventDate)) {
                mostRecentEffWorkflowVersion = productWorkflowDefVersion;
                break;
            }
        }
        assert mostRecentEffWorkflowVersion != null;
        return mostRecentEffWorkflowVersion;
    }

    public ProductWorkflowDefVersion getByVersion(String version) {
        return productDefVersionsByVersion.get(version);
    }

    @Nonnull
    public String getWorkflowImageFileName(Date effectiveDate) {
        return getName().replaceAll("\\s+", "_") + "_" + getEffectiveVersion(effectiveDate).getVersion() + "_" +
               effectiveDate.getTime() + WorkflowDiagramer.DIAGRAM_FILE_EXTENSION;
    }

    /** Returns the unique start dates for each product-process pairing, sorted by increasing date . */
    public List<Date> getEffectiveDates() {
        Set<Date> startDates = new HashSet<>();
        for (ProductWorkflowDefVersion workflow : getWorkflowVersionsDescEffDate()) {
            // Always uses the workflow effectiveDate.
            startDates.add(workflow.getEffectiveDate());
            for (WorkflowProcessDef wf : workflow.getWorkflowProcessDefs()) {
                for (WorkflowProcessDefVersion process : wf.getProcessVersionsDescEffDate()) {
                    if (workflow.getEffectiveDate().before(process.getEffectiveDate())) {
                        // Adds process effectiveDate if different from (after) the workflow start date.
                        startDates.add(process.getEffectiveDate());
                    }
                }
            }
        }
        List<Date> list = new ArrayList<>(startDates);
        Collections.sort(list);
        return list;
    }

}
