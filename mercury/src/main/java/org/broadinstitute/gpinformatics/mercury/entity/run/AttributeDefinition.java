package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This class defines the attributs of an AttributeArchetype. There are two types of attributes, one that
 * applies to one instance of an Archetype, and one that applies to one family of Archetypes.
 * The instance attributes have their names defined here, so that a UI can know what the required names
 * are, and their values get defined in ArchetypeAttribute, which has a link to one Archetype. Family-wide
 * attributes have both name and value defined here, which has a link to one family of Archetypes.
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
    private boolean isDisplayedInUi;
    /**
     * Indicates if the attribute is a family attribute (defined in this object) or
     * an instance attribute (defined in ArchetypeAttribute).
     */
    private boolean isFamilyAttribute;
    private String familyAttributeValue;

    public AttributeDefinition() {
    }

    /** Constructor for an Archetype instance attribute.
     * @param attributeFamily Defines the Archetype family.
     * @param attributeName Defines the name of the ArchetypeAttribute.
     * @param isDisplayedInUi Indicates if attribute is shown in the UI.
     */
    public AttributeDefinition(String attributeFamily, String attributeName, boolean isDisplayedInUi) {
        this.attributeFamily = attributeFamily;
        this.attributeName = attributeName;
        this.familyAttributeValue = null;
        this.isDisplayedInUi = isDisplayedInUi;
        this.isFamilyAttribute = false;
    }

    /** Constructor for an Archetype family attribute.
     * @param attributeFamily Defines the Archetype family that this attribute applies to.
     * @param attributeName Defines the name of the family attribute.
     * @param familyAttributeValue Defines the value of the attribute.
     * @param isDisplayedInUi Indicates if attribute is shown in the UI.
     */
    public AttributeDefinition(String attributeFamily, String attributeName, String familyAttributeValue,
                               boolean isDisplayedInUi) {
        this(attributeFamily, attributeName, isDisplayedInUi);
        this.familyAttributeValue = familyAttributeValue;
        this.isFamilyAttribute = true;
    }

    public String getAttributeFamily() {
        return attributeFamily;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getFamilyAttributeValue() {
        return familyAttributeValue;
    }

    public void setFamilyAttributeValue(String familyAttributeValue) {
        this.familyAttributeValue = familyAttributeValue;
    }

    public boolean isDisplayedInUi() {
        return isDisplayedInUi;
    }

    public void setIsDisplayedInUi(boolean isDisplayedInUi) {
        this.isDisplayedInUi = isDisplayedInUi;
    }

    public boolean isFamilyAttribute() {
        return isFamilyAttribute;
    }

    public void setIsFamilyAttribute(boolean isFamilyAttribute) {
        this.isFamilyAttribute = isFamilyAttribute;
    }
}
