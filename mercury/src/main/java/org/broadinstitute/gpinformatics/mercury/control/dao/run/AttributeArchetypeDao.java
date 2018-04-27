package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping_;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition_;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip_;
import org.broadinstitute.gpinformatics.mercury.entity.run.WorkflowMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.run.WorkflowMetadata_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Access Object for AttributeArchetype
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class AttributeArchetypeDao extends GenericDao {

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

    public List<GenotypingChip> findGenotypingChips() {
        return findAll((GenotypingChip.class));
    }

    public Collection<GenotypingChipMapping> findGenotypingChipMappings() {
        return findAll((GenotypingChipMapping.class));
    }

    private List<AttributeDefinition> findAttributeDefinitions(@NotNull AttributeDefinition.DefinitionType type) {
        return findList(AttributeDefinition.class, AttributeDefinition_.definitionType, type);
    }

    /** Returns the definitions for chip type attributes. */
    public Map<String, AttributeDefinition> findGenotypingChipAttributeDefinitions(String chipFamily) {
        Map<String, AttributeDefinition> map = new HashMap<>();
        for (AttributeDefinition def : findAttributeDefinitions(AttributeDefinition.DefinitionType.GENOTYPING_CHIP)) {
            if (chipFamily.equals(def.getGroup())) {
                map.put(def.getAttributeName(), def);
            }
        }
        return map;
    }

    /** Returns the named chip family attribute value. */
    public String findChipFamilyAttribute(String chipFamily, String attributeName) {
        for (AttributeDefinition def : findAttributeDefinitions(AttributeDefinition.DefinitionType.GENOTYPING_CHIP)) {
            if (def.isGroupAttribute() && attributeName.equals(def.getAttributeName()) &&
                def.getGroup().equals(chipFamily)) {
                return def.getGroupAttributeValue();
            }
        }
        return null;
    }

    /** Returns chip types for a chip family. */
    public Set<GenotypingChip> findGenotypingChips(String chipFamily) {
        Set<GenotypingChip> chips = new HashSet<>();
        for (GenotypingChip chip : findGenotypingChips()) {
            if (chip.getChipTechnology().equals(chipFamily)) {
                chips.add(chip);
            }
        }
        return chips;
    }

    /** Returns all chip families. */
    public Set<String> findGenotypingChipFamilies() {
        Set<String> families = new HashSet<>();
        for (GenotypingChip chip : findGenotypingChips()) {
            families.add(chip.getChipTechnology());
        }
        return families;
    }

    /** Returns the chip type */
    public GenotypingChip findGenotypingChip(String chipFamily, String chipName) {
        for (GenotypingChip chip : findList(GenotypingChip.class, GenotypingChip_.archetypeName, chipName)) {
            if (chip.getChipTechnology().equals(chipFamily)) {
                return chip;
            }
        }
        return null;
    }

    /**
     * Returns one mapping for each product (and possibly product order) found in the
     * genotyping chip mappings. The mappings are the ones active on the effectiveDate,
     * and if the effective date precedes all active dates the earliest mapping is used.
     *
     * @param effectiveDate comparison date. If null, returns the latest mappings, possibly inactive.
     * @return list of mappings
     */
    public Set<GenotypingChipMapping> getMappingsAsOf(Date effectiveDate) {
        Set<GenotypingChipMapping> activeMappings = new HashSet<>();

        Map<String, List<GenotypingChipMapping>> lookupKeyMappings = new HashMap<>();
        for (GenotypingChipMapping mapping : findGenotypingChipMappings()) {
            String lookupKey = mapping.getProductPartNumber() + "_/_" + mapping.getPdoSubstring();
            List<GenotypingChipMapping> list = lookupKeyMappings.get(lookupKey);
            if (list == null) {
                list = new ArrayList<>();
                lookupKeyMappings.put(lookupKey, list);
            }
            list.add(mapping);
        }

        // For each lookup key, sorts the mappings by date and finds the one active on the
        // the effective date, or uses the first active mapping.
        for (List<GenotypingChipMapping> list : lookupKeyMappings.values()) {
            Collections.sort(list, BY_DATE);
            GenotypingChipMapping bestMapping = null;
            for (GenotypingChipMapping mapping : list) {
                boolean isInactive = effectiveDate != null &&
                        (mapping.getInactiveDate() != null && !mapping.getInactiveDate().after(effectiveDate));
                boolean isActive = effectiveDate == null ||
                        (mapping.getActiveDate() != null && !mapping.getActiveDate().after(effectiveDate));
                if ((bestMapping == null || isActive) && !isInactive) {
                    bestMapping = mapping;
                }
            }
            if (bestMapping != null) {
                activeMappings.add(bestMapping);
            }
        }
        return activeMappings;
    }

    public Comparator<GenotypingChipMapping> BY_DATE = new Comparator<GenotypingChipMapping>() {
        @Override
        public int compare(GenotypingChipMapping o1, GenotypingChipMapping o2) {
            return o1.getActiveDate().compareTo(o2.getActiveDate());
        }
    };

    public GenotypingProductOrderMapping findGenotypingProductOrderMapping(Long productOrderId) {
        return findSingle(GenotypingProductOrderMapping.class, GenotypingProductOrderMapping_.archetypeName,
                productOrderId.toString());
    }

    public WorkflowMetadata findWorkflowMetadata(String workflowName) {
        return findSingle(WorkflowMetadata.class, WorkflowMetadata_.archetypeName, workflowName);
    }

    public Map<String, AttributeDefinition> findAttributeNamesByTypeAndGroup(
            AttributeDefinition.DefinitionType definitionType, String group) {
        Map<String, AttributeDefinition> map = new HashMap<>();
        for (AttributeDefinition def : findAttributeDefinitions(definitionType)) {
            if (group.equals(def.getGroup())) {
                map.put(def.getAttributeName(), def);
            }
        }
        return map;
    }

    /** Returns all key-value mapping entities for the given mapping name. */
    public List<KeyValueMapping> findKeyValueMappings(String mappingName) {
        return findList(KeyValueMapping.class, KeyValueMapping_.group, mappingName);
    }

    /** Returns one key-value mapping entities for the key and mapping name, or null if none found. */
    public KeyValueMapping findKeyValueByKeyAndMappingName(String key, String mappingName) {
        for (KeyValueMapping keyValueMapping : findKeyValueMappings(mappingName)) {
            if (keyValueMapping.getArchetypeName().equals(key)) {
                return keyValueMapping;
            }
        }
        return null;
    }

    /** Returns a Map for the given mapping name. */
    public Map<String, String> findKeyValueMap(String mappingName) {
        Map<String, String> map = new HashMap<>();
        for (KeyValueMapping keyValueMapping : findKeyValueMappings(mappingName)) {
            map.put(keyValueMapping.getKey(), keyValueMapping.getValue());
        }
        return map;
    }

}
