package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

/**
 * Contains the configurable attributes of a product, such as the mapping from
 * product to genotyping chip.
 */

@Audited
@Entity
public class GenotypingChipMapping extends AttributeArchetype {
    public final static String DELIMITER = " ";
    /** Archetype constants for the genotyping chip mapping. */
    public static final String GENOTYPING_CHIP_CONFIG = "Genotyping Chip";
    public static final String GENOTYPING_CHIP_NAME = "Genotyping chip name";
    public static final String GENOTYPING_CHIP_TECHNOLOGY = "Genotyping chip technology";
    // There is only one type of mapping.
    public static final String MAPPING_GROUP = "default";

    public GenotypingChipMapping() {
    }

    public GenotypingChipMapping(String mappingName) {
        super(GENOTYPING_CHIP_CONFIG, mappingName);
    }

    public GenotypingChipMapping(String mappingName, String chipTechnology, String chipName) {
        this(mappingName);
        addAttribute(GENOTYPING_CHIP_TECHNOLOGY, chipTechnology);
        addAttribute(GENOTYPING_CHIP_NAME, chipName);
    }

    public String getProductPartNumber() {
        return StringUtils.substringBefore(getArchetypeName(), DELIMITER);
    }

    public @NotNull String getPdoSubstring() {
        return StringUtils.trimToEmpty(StringUtils.substringAfter(getArchetypeName(), DELIMITER));
    }

    public String getChipFamily() {
        return getAttributeMap().get(GENOTYPING_CHIP_TECHNOLOGY);
    }

    public String getChipName() {
        return getAttributeMap().get(GENOTYPING_CHIP_NAME);
    }

    @Transient
    public void setChipTechnology(String chipTechnology) {
        setAttribute(GENOTYPING_CHIP_TECHNOLOGY, chipTechnology);
    }

    @Transient
    public void setChipName(String chipName) {
        setAttribute(GENOTYPING_CHIP_NAME, chipName);
    }
}
