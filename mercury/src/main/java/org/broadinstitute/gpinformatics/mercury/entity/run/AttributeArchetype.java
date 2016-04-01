package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AttributeArchetype is a class that can confer configurable attributes to some other entity.
 * An example is Genotyping array chip type, which is entirely modeled by an archetype, one per chip type,
 * and each archetype has a set of attributes for that chip type. Genotyping array chip type also has
 * a more global attribute that applies to each chip type family ("Infinium", "Fluidigm", etc.) and this
 * attribute is put in AttributeDefinition, keyed by attributeFamily.
 * The AttributeDefinition class defines the attribute names (the keys), and the AttributeArchetype
 * class contains each attribute key-value pair.
 *
 * Each instance of AttributeArchetype has a unique version (consisting of name and created date)
 * with its own set of attributes. It is intended to be immutable. Any edits made by a user to the
 * attribute values or any other fields should cause a new AttributeArchetype to be created, with the
 * old one still available for use if the application requires it, and not really a fit for Envers entity
 * auditing. It would be useful, for example, to obtain repeatable results when an older genotyping run
 * is reanalyzed with old chip attributes after the chip type attributes have been updated.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames =
        {"attributeFamily", "archetypeName", "createdDate"}))
public class AttributeArchetype {

    @SequenceGenerator(name = "seq_attribute_archetype", schema = "mercury", sequenceName = "seq_attribute_archetype")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_attribute_archetype")
    @Id
    private Long archetypeId;

    private String archetypeName;
    private String attributeFamily;
    private Date createdDate;
    /** Set this true to cause lookups to stop seeking an earlier version. */
    private boolean overridesEarlierVersions = false;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "archetype")
    @AuditJoinTable(name = "ARCHETYPE_JOIN_ATTRIBUTE_AUD")
    private List<ArchetypeAttribute> attributes;

    @Transient
    private List<AttributeDefinition> attributeDefinitions;

    public AttributeArchetype(String attributeFamily, String name) {
        this.attributeFamily = attributeFamily;
        this.archetypeName = name;
        createdDate = new Date();
        attributes = new ArrayList<>();
        attributeDefinitions = new ArrayList<>();
    }

    public String getArchetypeName() {
        return archetypeName;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public List<ArchetypeAttribute> getAttributes() {
        return attributes;
    }

    public List<AttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public boolean getOverridesEarlierVersions() {
        return overridesEarlierVersions;
    }

    public void setOverridesEarlierVersions(boolean overridesEarlierVersions) {
        this.overridesEarlierVersions = overridesEarlierVersions;
    }

    public Map<String, String> getAttributeMap() {
        return new HashMap<String, String>(){{
            for (ArchetypeAttribute attribute : attributes) {
                put(attribute.getAttributeName(), attribute.getAttributeValue());
            }
        }};
    }

    public String getAttributeFamily() {
        return attributeFamily;
    }

    public static final Comparator BY_NAME_DATE_DESC = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            AttributeArchetype g1 = (AttributeArchetype)o1;
            AttributeArchetype g2 = (AttributeArchetype)o2;
            int nameCompare = g1.archetypeName.compareTo(g2.archetypeName);
            return nameCompare != 0 ? nameCompare : g1.getCreatedDate().compareTo(g2.getCreatedDate());
        }
    };
}
