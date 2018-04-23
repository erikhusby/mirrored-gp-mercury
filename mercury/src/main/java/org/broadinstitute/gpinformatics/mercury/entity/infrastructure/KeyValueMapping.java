package org.broadinstitute.gpinformatics.mercury.entity.infrastructure;


import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Contains a generic key-value mapping backed by an AttributeArchetype.
 *
 * This is generic enough to be reused for any String to String Mapping:
 * There is one Map per attribute_group.
 * There is one AttributeArchetype for each map entry. The key is the archetype name.
 * The value comes from the linked ArchetypeAttribute having attribute_name 'theValue'.
 */

@Audited
@Entity
public class KeyValueMapping extends AttributeArchetype {
    // The first implementation of a KeyValueMapping.
    public static final String BAIT_PRODUCT_MAPPING = "BaitToProductMapping";

    private static final String GENERIC_KEY_VALUE_ATTRIBUTE = "theValue";

    @Transient
    public String getKey() {
        return getArchetypeName();
    }

    @Transient
    public String getValue() {
        ArchetypeAttribute archetypeAttribute = getAttribute(GENERIC_KEY_VALUE_ATTRIBUTE);
        return (archetypeAttribute != null) ? archetypeAttribute.getAttributeValue() : null;
    }
}
