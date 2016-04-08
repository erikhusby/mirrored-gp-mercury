package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype_;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for AttributeArchetype
 */
@Stateful
@RequestScoped
public class AttributeArchetypeDao extends GenericDao {

    /** Returns all archetypes for the given family. */
    public List<AttributeArchetype> findAllByFamily(String family) {
        return findList(AttributeArchetype.class, AttributeArchetype_.attributeFamily, family);
    }

    /** Returns one archetype for the given family and name, or null if not found. */
    public AttributeArchetype findByName(String family, String name) {
        for (AttributeArchetype attributeArchetype : findList(AttributeArchetype.class,
                AttributeArchetype_.archetypeName, name)) {
            if (attributeArchetype.getAttributeFamily().equals(family)) {
                return attributeArchetype;
            }
        }
        return null;
    }

    /** Returns the families having a given family attribute. */
    public List<String> findFamiliesIdentifiedByAttribute(String attributeName, String attributeValue) {
        List<String> familyNames;
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
        Root<AttributeDefinition> root = criteriaQuery.from(AttributeDefinition.class);
        criteriaQuery.select(root.get(AttributeDefinition_.attributeFamily));
        criteriaQuery.distinct(true);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(root.get(AttributeDefinition_.isFamilyAttribute), 1),
                criteriaBuilder.equal(root.get(AttributeDefinition_.attributeName), attributeName),
                criteriaBuilder.equal(root.get(AttributeDefinition_.familyAttributeValue), attributeValue))
        );
        criteriaQuery.orderBy(criteriaBuilder.asc(criteriaBuilder.upper(root.get(
                AttributeDefinition_.attributeFamily))));
        try {
            familyNames = getEntityManager().createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            familyNames = Collections.emptyList();
        }
        return familyNames;
    }

    /** Returns the attribute definitions for a family. Map is name to definition. */
    public Map<String, AttributeDefinition> findAttributeDefinitionsByFamily(String family) {
        Map<String, AttributeDefinition> map = new HashMap<>();
        for (AttributeDefinition definition : findList(AttributeDefinition.class,
                AttributeDefinition_.attributeFamily, family)) {
            map.put(definition.getAttributeName(), definition);
        }
        return map;
    }

    /** Returns one attribute definition for a family and attribute name, or null if not found. */
    public AttributeDefinition findAttributeDefinitionByFamily(String family, String attributeName) {
        for (AttributeDefinition def :
                findList(AttributeDefinition.class, AttributeDefinition_.attributeFamily, family)) {
            if (def.getAttributeName().equals(attributeName)) {
                return def;
            }
        }
        return null;
    }
}
