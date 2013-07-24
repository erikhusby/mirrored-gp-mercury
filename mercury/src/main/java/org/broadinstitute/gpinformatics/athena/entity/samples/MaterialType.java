package org.broadinstitute.gpinformatics.athena.entity.samples;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Entity for Sample Material Type.
 *
 * @author mccrory
 */
@Entity
@Audited
@Table(schema = "athena",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "category"})
        })
public class MaterialType implements Serializable, Comparable<MaterialType> {

    @Id
    @SequenceGenerator(name = "SEQ_MATERIAL_TYPE", schema = "athena", sequenceName = "SEQ_MATERIAL_TYPE", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MATERIAL_TYPE")
    private Long materialTypeId;

    @Column(name = "NAME",nullable = false)
    private String name;

    @Column(name = "CATEGORY", nullable = false)
    private String category;


    public static final Comparator<MaterialType> MATERIAL_TYPE_COMPARATOR = new Comparator<MaterialType>() {
        @Override
        public int compare(MaterialType materialType, MaterialType MaterialType1) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(materialType.getName(), MaterialType1.getName());
            builder.append(materialType.getCategory(), MaterialType1.getCategory());
            return builder.build();
        }
    };

    /**
     * Package visible constructor for JPA
     */
    MaterialType() {
    }

    public MaterialType(@Nonnull String category, @Nonnull String name ) {
        if (name == null) {
            throw new NullPointerException("Null name!");
        }
        if (category == null) {
            throw new NullPointerException("Null category!");
        }
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    // Mirror BSP's full name method of adding colon in between the names
    public String getFullName() {
        return category + ":" + name;
    }

    @Override
    public int compareTo(MaterialType that) {
        return MATERIAL_TYPE_COMPARATOR.compare(this, that);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MaterialType)) {
            return false;
        }

        MaterialType materialType = (MaterialType) o;

        return new EqualsBuilder().append(name, materialType.getName())
                .append(category, materialType.getCategory()).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(category).hashCode();
    }

}
