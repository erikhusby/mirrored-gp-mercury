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
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AttributeArchetype is intended to confer configurable attributes to some other entity, for the
 * purpose of adding properties via fixup test rather than hotfix.
 * An archetype may also be sufficient by itself without needing another entity.
 * An example is Genotyping array chip type, which models a chip vendor/technology (e.g. Infinium)
 * as an archetype group and the genotyping chip types as archetypes of that group.
 * Attributes that apply to a specific chip are represented in ArchetypeAttribute.
 * Their names and UI visibility are represented in AttributeDefinition.
 * Attributes that apply to an archetype group are also represented in AttributeDefinition.
  */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"archetype_group", "namespace", "archetypeName"}))
public class AttributeArchetype {

    @SequenceGenerator(name = "seq_attribute_archetype", schema = "mercury", sequenceName = "seq_attribute_archetype")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_archetype")
    @Id
    private Long archetypeId;

    private String namespace;

    @Column(name = "archetype_group")
    private String group;

    private String archetypeName;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "archetype")
    private Set<ArchetypeAttribute> attributes;

    public AttributeArchetype() {
    }

    public AttributeArchetype(String namespace, String group, String name) {
        this.namespace = namespace;
        this.group = group;
        this.archetypeName = name;
        attributes = new HashSet<>();
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public Set<ArchetypeAttribute> getAttributes() {
        return attributes;
    }

    public String getNamespace() {
        return namespace;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttributeArchetype)) {
            return false;
        }

        AttributeArchetype archetype = (AttributeArchetype) o;

        if (archetypeId != null ? !archetypeId.equals(archetype.archetypeId) : archetype.archetypeId != null) {
            return false;
        }
        if (archetypeName != null ? !archetypeName.equals(archetype.archetypeName) : archetype.archetypeName != null) {
            return false;
        }
        if (namespace != null ? !namespace.equals(archetype.namespace) :
                archetype.namespace != null) {
            return false;
        }
        if (group != null ? !group.equals(archetype.group) :
                archetype.group != null) {
            return false;
        }
        // Uses attribute id, name, value and archetypeId in the comparison.
        if (attributes == null) {
            return (archetype.attributes == null);
        } else if (archetype.attributes == null) {
            return false;
        }
        Map<String, ArchetypeAttribute> map = new HashMap<>();
        for (ArchetypeAttribute attribute : attributes) {
            map.put(attribute.getAttributeName(), attribute);
        }
        for (ArchetypeAttribute otherAttribute : archetype.attributes) {
            ArchetypeAttribute attribute = map.get(otherAttribute.getAttributeName());
            if (!otherAttribute.equals(attribute)) {
                return false;
            }
        }
        return (map.size() == attributes.size());
    }

    @Override
    public int hashCode() {
        int result = archetypeId != null ? archetypeId.hashCode() : 0;
        result = 31 * result + (archetypeName != null ? archetypeName.hashCode() : 0);
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (group != null ? group.hashCode() : 0);
        List<Integer> attributeHashcodes = new ArrayList<>();
        for (ArchetypeAttribute attribute : attributes) {
            attributeHashcodes.add(attribute.hashCode());
        }
        Collections.sort(attributeHashcodes);
        result = 31 * result + attributeHashcodes.hashCode();
        return result;
    }
}
