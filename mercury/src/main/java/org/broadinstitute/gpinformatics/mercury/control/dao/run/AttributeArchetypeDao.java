package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for AttributeArchetype
 */
@Stateful
@RequestScoped
public class AttributeArchetypeDao extends GenericDao {

    /**
     * Returns all archetypes for the given family.
     */
    public List<AttributeArchetype> findAllByFamily(String family) {
        return findList(AttributeArchetype.class, AttributeArchetype_.attributeFamily, family);
    }

    /**
     * Returns the most recent version of AttributeArchetype having the specified name, or null if none found.
     */
    public AttributeArchetype findByName(String family, String name) {
        List<AttributeArchetype> list = findAllVersionsByFamilyAndName(family, name);
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /**
     * Returns the AttributeArchetype having a version that encompasses the effectiveDate
     * i.e. the most recent archetype version that is before or on the effectiveDate.
     * If a version is encountered that indicates it should override earlier versions, then
     * that version is returned.
     */
    public AttributeArchetype findByName(String family, String name, Date effectiveDate) {
        for (AttributeArchetype attributeArchetype : findAllVersionsByFamilyAndName(family, name)) {
            if (attributeArchetype.getOverridesEarlierVersions() ||
                !attributeArchetype.getCreatedDate().after(effectiveDate)) {
                return attributeArchetype;
            }
        }
        return null;
    }

    /** Returns all versions of an AttributeArchetype, sorted by created date desc (most recent first). */
    public List<AttributeArchetype> findAllVersionsByFamilyAndName(String family, String name) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<AttributeArchetype> criteriaQuery = criteriaBuilder.createQuery(AttributeArchetype.class);
        Root<AttributeArchetype> root = criteriaQuery.from(AttributeArchetype.class);
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(root.get(AttributeArchetype_.attributeFamily), family),
                criteriaBuilder.equal(root.get(AttributeArchetype_.archetypeName), name)));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(AttributeArchetype_.createdDate)));
        return getEntityManager().createQuery(criteriaQuery).getResultList();
    }

    /** Returns the family names for family attributes having the specified name and value. */
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

    /**
     * Returns all of the attribute definitions for an archetype family.
     */
    public List<AttributeDefinition> findAttributeDefinitionsByFamily(String family) {
        return findList(AttributeDefinition.class, AttributeDefinition_.attributeFamily, family);
    }

    /**
     * Returns the specific attribute definition for an archetype family, or null if not found.
     */
    public AttributeDefinition findAttributeDefinitionByFamily(String family, String attributeName) {
        for (AttributeDefinition def :
                findList(AttributeDefinition.class, AttributeDefinition_.attributeFamily, family)) {
            if (def.getAttributeName().equals(attributeName)) {
                return def;
            }
        }
        return null;
    }

    /**
     * Creates a new archetype version using the new attribute values, if one or more of the attribute
     * values are different from the latest available version.
     * @param family the existingArchetype family.
     * @param name the existingArchetype name.
     * @param attributeMap map of attributeName -> newAttributeValue.
     * @return the create new version, or null if no changes were found.
     */
    public AttributeArchetype createArchetypeVersion(String family, String name, Map<String, String> attributeMap) {
        boolean foundChange = false;
        AttributeArchetype existingArchetype = findByName(family, name);
        for (ArchetypeAttribute attribute : existingArchetype.getAttributes()) {
            String newValue = attributeMap.get(attribute.getAttributeName());
            String oldValue = attribute.getAttributeValue();
            if (oldValue == null && newValue != null || oldValue != null && !oldValue.equals(newValue)) {
                foundChange = true;
                break;
            }
        }
        if (foundChange) {
            AttributeArchetype newVersion = new AttributeArchetype(family, name);
            persist(newVersion);
            // Must flush here to define archetypeId and avoid subsequent ArchetypeAttribute unique key failure.
            flush();

            for (ArchetypeAttribute oldAttribute : existingArchetype.getAttributes()) {
                String newData = attributeMap.get(oldAttribute.getAttributeName());
                ArchetypeAttribute newAttribute =
                        new ArchetypeAttribute(newVersion, oldAttribute.getAttributeName(), newData);
                newVersion.getAttributes().add(newAttribute);
            }
            return newVersion;
        }
        return null;
    }

}
