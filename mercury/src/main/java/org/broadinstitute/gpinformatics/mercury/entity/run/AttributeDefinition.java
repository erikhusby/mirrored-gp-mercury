package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"definition_type", "archetype_group", "attribute_name"}))
public class AttributeDefinition {

    public enum DefinitionType {
        GENOTYPING_CHIP,
        GENOTYPING_CHIP_MAPPING,
        GENOTYPING_PRODUCT_ORDER,
        WORKFLOW_METADATA,
        KEY_VALUE_MAPPING
    }

    @SequenceGenerator(name = "seq_attribute_definition", schema = "mercury", sequenceName = "seq_attribute_definition")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_definition")
    @Id
    private Long attributeDefinitionId;

    @Enumerated(EnumType.STRING)
    private DefinitionType definitionType;

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

    /** Constructor for attributes that vary by archetype instance. Values are in ArchetypeAttribute. */
    private AttributeDefinition(DefinitionType definitionType, String group, String attributeName) {
        this.definitionType = definitionType;
        this.group = group;
        this.attributeName = attributeName;
    }

    /** Constructor for attributes that vary by archetype group. */
    public AttributeDefinition(DefinitionType definitionType, String group, String attributeName,
                               String groupAttributeValue) {
        this(definitionType, group, attributeName);
        this.groupAttributeValue = groupAttributeValue;
        this.isGroupAttribute = true;
    }

    /** Constructor for attributes that vary by archetype instance that are to be displayed. */
    public AttributeDefinition(DefinitionType definitionType, String group, String attributeName,
                               boolean isDisplayable) {
        this.definitionType = definitionType;
        this.group = group;
        this.attributeName = attributeName;
        this.isGroupAttribute = false;
        this.isDisplayable = isDisplayable;
    }

    public String getGroup() {
        return group;
    }

    /**
     * Fixup test requires access
     */
    public void setGroup( String group ) {
        this.group = group;
    }

    public DefinitionType getDefinitionType() {
        return definitionType;
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
