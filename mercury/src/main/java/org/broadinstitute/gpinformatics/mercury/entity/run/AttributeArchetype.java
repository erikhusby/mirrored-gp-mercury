package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The AttributeArchetype is a class that can confer configurable attributes to some other entity.
 * An example is Genotyping array chip type, which is entirely modeled by an archetype, one per chip type,
 * and each archetype has a set of attributes for that chip type. Genotyping array chip type also has
 * a more global attribute that applies to each chip type family ("Infinium", "Fluidigm", etc.) and this
 * attribute is put in AttributeDefinition, keyed by attributeFamily.
 * The AttributeDefinition class defines the attribute names (the keys), and the AttributeArchetype
 * class contains each attribute key-value pair.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"attributeFamily", "archetypeName"}))
public class AttributeArchetype {

    @SequenceGenerator(name = "seq_attribute_archetype", schema = "mercury", sequenceName = "seq_attribute_archetype")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_archetype")
    @Id
    private Long archetypeId;

    private String archetypeName;
    private String attributeFamily;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "archetype")
    private Set<ArchetypeAttribute> attributes;

    public AttributeArchetype() {
    }

    public AttributeArchetype(String attributeFamily, String name) {
        this.attributeFamily = attributeFamily;
        this.archetypeName = name;
        attributes = new HashSet<>();
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public Set<ArchetypeAttribute> getAttributes() {
        return attributes;
    }

    public String getAttributeFamily() {
        return attributeFamily;
    }

    public Map<String, String> getAttributeMap() {
        return new HashMap<String, String>(){{
            for (ArchetypeAttribute attribute : attributes) {
                put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }};
    }
}
