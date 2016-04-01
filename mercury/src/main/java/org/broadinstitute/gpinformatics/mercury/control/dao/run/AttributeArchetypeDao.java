package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Data Access Object for AttributeArchetype
 */
@Stateful
@RequestScoped
public class AttributeArchetypeDao extends GenericDao {

    /**
     * Returns all attribute family names starting with the given prefix.
     * When prefix=null returns all family names.
     */
    public List<String> findFamilyNamesByPrefix(String prefix) {
        List<String> familyNames;
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<String> criteriaQuery = criteriaBuilder.createQuery(String.class);
        Root<AttributeDefinition> root = criteriaQuery.from(AttributeDefinition.class);
        criteriaQuery.select(root.get(AttributeDefinition_.attributeFamily));
        criteriaQuery.distinct(true);
        criteriaQuery.where(criteriaBuilder.like(root.get(AttributeDefinition_.attributeFamily), prefix + "%"));
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

    /**
     * Returns all of the attribute definitions for an archetype family.
     */
    public List<AttributeDefinition> findAttributeDefinitionsByFamily(String family) {
        return findList(AttributeDefinition.class, AttributeDefinition_.attributeFamily, family);
    }

    /**
     * Returns the specific attribute definition for an archetype family, or null if not found.
     */
    public AttributeDefinition findAttributeDefinitionsByFamily(String family, String attributeName) {
        for (AttributeDefinition def :
                findList(AttributeDefinition.class, AttributeDefinition_.attributeFamily, family)) {
            if (def.getAttributeName().equals(attributeName)) {
                return def;
            }
        }
        return null;
    }

}
