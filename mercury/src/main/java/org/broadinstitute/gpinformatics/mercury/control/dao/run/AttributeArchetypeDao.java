package org.broadinstitute.gpinformatics.mercury.control.dao.run;

import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingChipMapping;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition_;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Comparator;
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
            if (def.isGroupAttribute() && attributeName.equals(def.getAttributeName())) {
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
    public GenotypingChip findGenotypingChip(@NotNull String chipFamily, String chipName) {
        for (GenotypingChip chip : findList(GenotypingChip.class, GenotypingChip_.archetypeName, chipName)) {
            if (chip.getChipTechnology().equals(chipFamily)) {
                return chip;
            }
        }
        return null;
    }

}
