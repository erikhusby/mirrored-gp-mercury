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
 * This class defines the name and other characteristics of an AttributeArchetype.
 * For group attributes it also holds the attribute value.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"archetype_group", "namespace", "attributeName"}))
public class AttributeDefinition {

    @SequenceGenerator(name = "seq_attribute_definition", schema = "mercury", sequenceName = "seq_attribute_definition")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_definition")
    @Id
    private Long attributeDefinitionId;

    private String namespace;

    @Column(name = "archetype_group")
    private String group;

    private String attributeName;

    private boolean isDisplayable;

    /**
     * Indicates if the attribute is a group attribute (defined in this object) or
     * an instance attribute (defined in ArchetypeAttribute).
     */
    @Column(name = "is_group_attribute")
    private boolean isGroupAttribute;

    private String groupAttributeValue;

    public AttributeDefinition() {
    }

    public AttributeDefinition(String namespace, String group, String attributeName, String groupAttributeValue,
                               boolean isDisplayable, boolean isGroupAttribute) {
        this.namespace = namespace;
        this.group = group;
        this.attributeName = attributeName;
        this.groupAttributeValue = groupAttributeValue;
        this.isDisplayable = isDisplayable;
        this.isGroupAttribute = isGroupAttribute;
    }

    public String getGroup() {
        return group;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getGroupAttributeValue() {
        return groupAttributeValue;
    }

    public void setGroupAttributeValue(String groupAttributeValue) {
        this.groupAttributeValue = groupAttributeValue;
    }

    public boolean isDisplayable() {
        return isDisplayable;
    }

    public void setIsDisplayable(boolean isDisplayable) {
        this.isDisplayable = isDisplayable;
    }

    public boolean isGroupAttribute() {
        return isGroupAttribute;
    }

    public void setIsGroupAttribute(boolean isGroupAttribute) {
        this.isGroupAttribute = isGroupAttribute;
    }
}
