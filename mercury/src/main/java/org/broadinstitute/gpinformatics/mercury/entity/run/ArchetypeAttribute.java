package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.text.ParseException;
import java.util.Date;

/**
 * The attributes of an AttributeArchetype are represented here by free-form key value pairs.
 */

@Entity
@Audited
@Table(schema = "mercury", uniqueConstraints = @UniqueConstraint(columnNames = {"ARCHETYPE","ATTRIBUTE_NAME"}))
public class ArchetypeAttribute {
    public static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    @SequenceGenerator(name = "seq_archetype_attribute", schema = "mercury", sequenceName = "seq_archetype_attribute")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_archetype_attribute")
    @Id
    private Long attributeId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "ARCHETYPE")
    private AttributeArchetype archetype;

    private String attributeName;

    private String attributeValue;

    public ArchetypeAttribute() {
    }

    public ArchetypeAttribute(AttributeArchetype archetype, String attributeName) {
        this.archetype = archetype;
        this.attributeName = attributeName;
    }

    public ArchetypeAttribute(AttributeArchetype archetype, String attributeName, String attributeValue) {
        this(archetype, attributeName);
        this.attributeValue = attributeValue;
    }

    public Long getAttributeId() {
        return attributeId;
    }

    public AttributeArchetype getArchetype() {
        return archetype;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    /**
     * Returns value as a date. Returns null if unset or if value is not a date.
     */
    public Date getDate() {
        try {
            return StringUtils.isNotBlank(attributeValue) ? dateFormat.parse(attributeValue) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Sets value as a date.
     */
    @Transient
    public void setDate(Date date) {
        attributeValue = (date != null) ? dateFormat.format(date) : "";
    }

}
