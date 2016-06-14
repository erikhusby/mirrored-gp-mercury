package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Date;

/**
 * Represents a Genotyping chip.
 * An example of chip vendor/technology/family is Infinium. This is represented in the archetype group.
 */

@Audited
@Entity
public class GenotypingChip extends AttributeArchetype {
    public static final String LAST_MODIFIED = "LastModifiedDate";

    public GenotypingChip() {
    }

    public GenotypingChip(String chipFamily, String chipName, Collection<AttributeDefinition> attributeDefinitions) {
        super(chipFamily, chipName, attributeDefinitions);
    }

    public String getChipTechnology() {
        return getGroup();
    }

    public String getChipName() {
        return getArchetypeName();
    }

    @Transient
    public void setLastModifiedDate() {
        addOrSetAttribute(LAST_MODIFIED, DateUtils.getYYYYMMMDDTime(new Date()));
    }
}
