package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This class defines the attribute names, plus any common data, used by each attribute family.
 * It can be thought of as a persisted enum of attribute names, plus any class fields that would
 * apply to all instances of the attribute family.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"attributeFamily", "attributeName"}))
public class AttributeDefinition {

    @SequenceGenerator(name = "seq_attribute_definition", schema = "mercury", sequenceName = "seq_attribute_definition")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_definition")
    @Id
    private Long attributeDefinitionId;

    private String attributeFamily;
    private String attributeName;

    /**
     * The value in this field applies to all instances of AttributeArchetype that have this attributeFamily.
     *
     * <b>Leave this NULL to indicate that this attribute gets defined in ArchetypeAttribute.</b>
     */
    @Column(name = "classFieldValue")
    private String attributeFamilyClassFieldValue;

    public AttributeDefinition(String attributeFamily, String attributeName) {
        this.attributeFamily = attributeFamily;
        this.attributeName = attributeName;
    }

    public AttributeDefinition(String attributeFamily, String attributeName, String attributeFamilyClassFieldValue) {
        this(attributeFamily, attributeName);
        this.attributeFamilyClassFieldValue = attributeFamilyClassFieldValue;
    }

    public String getAttributeFamily() {
        return attributeFamily;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeFamilyClassFieldValue() {
        return attributeFamilyClassFieldValue;
    }

    public void setAttributeFamilyClassFieldValue(String attributeFamilyClassFieldValue) {
        this.attributeFamilyClassFieldValue = attributeFamilyClassFieldValue;
    }
}
