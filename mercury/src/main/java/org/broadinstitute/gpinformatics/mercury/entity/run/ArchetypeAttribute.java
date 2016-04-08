package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * The attributes of an AttributeArchetype are represented here by free-form key value pairs.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"archetype","attributeName"}))
public class ArchetypeAttribute {

    @SequenceGenerator(name = "seq_archetype_attribute", schema = "mercury", sequenceName = "seq_archetype_attribute")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_archetype_attribute")
    @Id
    private Long attributeId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private AttributeArchetype archetype;

    private String attributeName;

    private String attributeValue;

    public ArchetypeAttribute() {
    }

    public ArchetypeAttribute(AttributeArchetype archetype, String attributeName) {
        this.archetype = archetype;
        this.attributeName = attributeName;
    }

    public ArchetypeAttribute(AttributeArchetype archetype, String attributeName, String attributeValue) {
        this(archetype, attributeName);
        this.attributeValue = attributeValue;
    }

    public AttributeArchetype getArchetype() {
        return archetype;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }
}
