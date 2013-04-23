package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

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

    private String routingRule;


    /** List of versions */
    private List<ProductWorkflowDefVersion> productWorkflowDefVersions = new ArrayList<ProductWorkflowDefVersion>();

    /** Transient list of versions, in descending order of effective date */
    private transient ArrayList<ProductWorkflowDefVersion> workflowVersionsDescEffDate;
    private transient Map<String, ProductWorkflowDefVersion> productDefVersionsByVersion =
            new HashMap<String, ProductWorkflowDefVersion>();

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
            workflowVersionsDescEffDate = new ArrayList<ProductWorkflowDefVersion>(productWorkflowDefVersions);
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
            if(productWorkflowDefVersion.getEffectiveDate().before(eventDate)) {
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

    /**
     * Accessor for the routing logic associated with a workflow instance.  The routing logic helps the system determine
     * which LIMS system ({@see MercuryOrSquid}) should be considered the primary system of record.  Based on this
     * value, mercury will either keep all LIMS related information to itself, share that information with another
     * system, or pass all information to another system
     *
     * @return String to represent the routing intent.  Values are based on enums found in
     * {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid}
     */
    public String getRoutingRule() {
        return routingRule;
    }

    /**
     * This method is an extension of {@link #getRoutingRule()}.  Since the values defined in the routingRule are
     * based on {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid},
     * this method helps solidify that point.  It provides the user with an interpretation of the routing rule
     * in the form of a MercuryOrSquid enum
     *
     * @return an instance of
     * {@link org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid} that
     * corresponds to the String value found in the routing rule.
     */
    public MercuryOrSquidRouter.MercuryOrSquid getRouting() {
        return MercuryOrSquidRouter.MercuryOrSquid.valueOf(getRoutingRule());
    }

}
