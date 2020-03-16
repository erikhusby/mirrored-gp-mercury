/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.stream.Stream;

@Audited
@Entity
public class BillingTriggerMapping extends AttributeArchetype {
    public BillingTriggerMapping() {
    }

    public static final String BILLING_TRIGGER = "Billing Trigger";
    public static final String AOU_PDO_BILLING = "AOU_PDO_BILLING";
    public BillingTriggerMapping(String group, String name) {
        super(group, name);
    }

    public BillingTriggerMapping(String group, String name,
                                 Collection<AttributeDefinition> attributeDefinitions) {
        super(group, name, attributeDefinitions);
    }

    @Transient
    public ResearchProject.BillingTrigger getBillingTriggerAttribute() {
        String attributeValue = super.getAttribute(BILLING_TRIGGER).getAttributeValue();
        ResearchProject.BillingTrigger billingTrigger = Stream.of(ResearchProject.BillingTrigger.values())
            .filter(s -> s.name().equals(attributeValue)).findFirst().orElse(ResearchProject.BillingTrigger.NONE);

        return billingTrigger;
    }
}
