package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype_;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Data Access Object for AttributeArchetype
 */
@Stateful
@RequestScoped
public class AttributeArchetypeDao extends GenericDao {

    public Set<String> findGroups(@NotNull String namespace) {
        Set<String> groupNames = new HashSet<>();
        for (AttributeArchetype archetype : findList(AttributeArchetype.class, AttributeArchetype_.namespace, namespace)) {
            groupNames.add(archetype.getGroup());
        }
        return groupNames;
    }

    /** Returns all archetypes for the given group. */
    public Set<AttributeArchetype> findByGroup(@NotNull String namespace, @NotNull String group) {
        Set<AttributeArchetype> archetypes = new HashSet<>();
        for (AttributeArchetype archetype : findList(AttributeArchetype.class, AttributeArchetype_.group, group)) {
            if (namespace.equals(archetype.getNamespace())) {
                archetypes.add(archetype);
            }
        }
        return archetypes;
    }

    /** Returns one archetype for the given group and name, or null if not found. */
    public AttributeArchetype findByName(@NotNull String namespace, @NotNull String group, @NotNull String name) {
        for (AttributeArchetype attributeArchetype : findList(AttributeArchetype.class,
                AttributeArchetype_.archetypeName, name)) {
            if (attributeArchetype.getGroup().equals(group)) {
                return attributeArchetype;
            }
        }
        return null;
    }

    /** Returns a map (name -> definition) of the attribute definitions for an attribute group. */
    public Map<String, AttributeDefinition> findAttributeDefinitions(@NotNull String namespace, @NotNull String group) {
        Map<String, AttributeDefinition> map = new HashMap<>();
        for (AttributeDefinition definition : findList(AttributeDefinition.class,
                AttributeDefinition_.group, group)) {
            if (namespace.equals(definition.getNamespace())) {
                map.put(definition.getAttributeName(), definition);
            }
        }
        return map;
    }

    public static final Comparator<AttributeArchetype> BY_ARCHETYPE_NAME = new Comparator<AttributeArchetype>() {
        @Override
        public int compare(AttributeArchetype o1, AttributeArchetype o2) {
            if (o1 != null) {
                return (o2 != null) ? o1.getArchetypeName().compareTo(o2.getArchetypeName()) : 1;
            } else {
                return (o2 != null) ? -1 : 0;
            }
        }
    };

}
