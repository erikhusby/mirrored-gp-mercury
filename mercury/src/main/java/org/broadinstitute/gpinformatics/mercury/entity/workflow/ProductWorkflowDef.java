package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
public class ProductWorkflowDef {

    /** e.g. Exome Express */
    private String name;

    /** List of versions */
    List<ProductWorkflowDefVersion> productWorkflowDefVersions = new ArrayList<ProductWorkflowDefVersion>();

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

    public List<String> validate(LabVessel labVessel, String nextEventTypeName) {
        List<String> errors = new ArrayList<String>();
        ProductWorkflowDefVersion effectiveWorkflowDef = getEffectiveVersion();

        ProductWorkflowDefVersion.LabEventNode labEventNode = effectiveWorkflowDef.findStepByEventType(nextEventTypeName);
        if(labEventNode == null) {
            errors.add("Failed to find " + nextEventTypeName + " in " + name + " version " + effectiveWorkflowDef.getVersion());
        } else {
            Set<String> actualEventNames = new HashSet<String>();
            boolean found = false;

            boolean start = labEventNode.getPredecessors().size() == 0;
            Set<String> validPredecessorEventNames = new HashSet<String>();
            for (ProductWorkflowDefVersion.LabEventNode predecessorNode : labEventNode.getPredecessors()) {
                validPredecessorEventNames.add(predecessorNode.getLabEventType().getName());
                // todo jmt recurse
                if(predecessorNode.isOptional()) {
                    start = predecessorNode.getPredecessors().size() == 0;
                    for (ProductWorkflowDefVersion.LabEventNode predPredEventNode : predecessorNode.getPredecessors()) {
                        validPredecessorEventNames.add(predPredEventNode.getLabEventType().getName());
                    }
                }
            }

            found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                    found, labVessel.getTransfersFrom());

            if (!found) {
                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                        found, labVessel.getTransfersTo());
            }
            if(!found) {
                found = validateTransfers(nextEventTypeName, errors, validPredecessorEventNames, labVessel, actualEventNames,
                        found, labVessel.getInPlaceEvents());
            }
            if(!found && !start) {
                errors.add("Vessel " + labVessel.getLabCentricName() + " has actual events " + actualEventNames +
                        ", but none are predecessors to " + nextEventTypeName + ": " + validPredecessorEventNames);
            }
        }
        return errors;
    }

    private boolean validateTransfers(String nextEventTypeName, List<String> errors, Set<String> validPredecessorEventNames,
            LabVessel labVessel, Set<String> actualEventNames, boolean found, Set<LabEvent> transfers) {
        for (LabEvent labEvent : transfers) {
            String actualEventName = labEvent.getLabEventType().getName();
            actualEventNames.add(actualEventName);
/*
            if(actualEventName.equals(nextEventTypeName)) {
                errors.add("For vessel " + labVessel.getLabCentricName() + ", event " + nextEventTypeName + " has already occurred");
            }
*/
            if(validPredecessorEventNames.contains(actualEventName)) {
                found = true;
                break;
            }
        }
        return found;
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

    public ProductWorkflowDefVersion getByVersion(String version) {
        return productDefVersionsByVersion.get(version);
    }


}
