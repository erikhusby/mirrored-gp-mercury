package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.Date;

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
    public static final String ACTIVE_DATE = "Active date";
    public static final String INACTIVE_DATE = "Inactive date";
    // There is only one type of mapping.
    public static final String MAPPING_GROUP = "default";

    public GenotypingChipMapping() {
    }

    public GenotypingChipMapping(String mappingName, String chipTechnology, String chipName, Date effectiveDate) {
        super(GENOTYPING_CHIP_CONFIG, mappingName);
        addOrSetAttribute(GENOTYPING_CHIP_TECHNOLOGY, chipTechnology);
        addOrSetAttribute(GENOTYPING_CHIP_NAME, chipName);
        addOrSetAttribute(ACTIVE_DATE, "");
        setActiveDate(effectiveDate);
        addOrSetAttribute(INACTIVE_DATE, "");
    }

    public String getProductPartNumber() {
        return StringUtils.substringBefore(getArchetypeName(), DELIMITER);
    }

    public
    @NotNull
    String getPdoSubstring() {
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
        addOrSetAttribute(GENOTYPING_CHIP_TECHNOLOGY, chipTechnology);
    }

    @Transient
    public void setChipName(String chipName) {
        addOrSetAttribute(GENOTYPING_CHIP_NAME, chipName);
    }

    public Date getActiveDate() {
        return getAttribute(ACTIVE_DATE).getDate();
    }

    @Transient
    public void setActiveDate(Date date) {
        getAttribute(ACTIVE_DATE).setDate(date);
    }

    public Date getInactiveDate() {
        return getAttribute(INACTIVE_DATE).getDate();
    }

    @Transient
    public void setInactiveDate(Date date) {
        getAttribute(INACTIVE_DATE).setDate(date);
    }

}
