package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AttributeArchetype is intended to confer configurable attributes to some other entity, for the
 * purpose of adding properties via fixup test rather than hotfix.
 */

@Entity
@Audited
@Table(schema = "mercury")
public abstract class AttributeArchetype {

    @SequenceGenerator(name = "seq_attribute_archetype", schema = "mercury", sequenceName = "seq_attribute_archetype")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_archetype")
    @Id
    private Long archetypeId;

    @Column(name = "archetype_group")
    private String group;

    private String archetypeName;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "archetype")
    private Set<ArchetypeAttribute> attributes;

    public AttributeArchetype() {
    }

    public AttributeArchetype(String group, String name) {
        this.group = group;
        this.archetypeName = name;
        attributes = new HashSet<>();
    }

    /** Creates archtype and adds the instance attributes with null values. */
    public AttributeArchetype(String group, String name, Collection<AttributeDefinition> attributeDefinitions) {
        this(group, name);
        for (AttributeDefinition definition : attributeDefinitions) {
            if (!definition.isGroupAttribute()) {
                addOrSetAttribute(definition.getAttributeName(), null);
            }
        }
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public Set<ArchetypeAttribute> getAttributes() {
        return attributes;
    }

    public String getGroup() {
        return group;
    }

    public Long getArchetypeId() {
        return archetypeId;
    }

    public Map<String, String> getAttributeMap() {
        return new HashMap<String, String>(){{
            if (attributes != null) {
                for (ArchetypeAttribute attribute : attributes) {
                    put(attribute.getAttributeName(), attribute.getAttributeValue());
                }
            }
        }};
    }

    public ArchetypeAttribute getAttribute(String attributeName) {
        if (attributes != null) {
            for (ArchetypeAttribute attribute : attributes) {
                if (attribute.getAttributeName().equals(attributeName)) {
                    return attribute;
                }
            }
        }
        return null;
    }

    public void addOrSetAttribute(@NotNull String name, String value) {
        ArchetypeAttribute attribute = getAttribute(name);
        // If it already exists then sets the value.
        if (attribute == null) {
            attributes.add(new ArchetypeAttribute(this, name, value));
        } else {
            attribute.setAttributeValue(value);
        }
    }

}
