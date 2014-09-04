package org.broadinstitute.gpinformatics.mercury.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Basic metadata storage class with String keys and values.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "METADATA")
public class Metadata {

    @Id
    @SequenceGenerator(name = "SEQ_METADATA", schema = "mercury", sequenceName = "SEQ_METADATA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_METADATA")
    @SuppressWarnings("UnusedDeclaration")
    @Column(name = "metadata_id")
    private Long id;

    @Column(name = "key")
    @Enumerated(EnumType.STRING)
    private Key key;

    @Column(name = "value")
    private String value;

    /**
     * For JPA
     */
    protected Metadata() {
    }

    public Metadata(@Nonnull Key key, String value) {
        this.key = key;
        this.value = value;
    }

    public Key getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Metadata metadata = (Metadata) o;

        return new EqualsBuilder().append(key, metadata.getKey()).append(value, metadata.getValue()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(key).append(value).hashCode();
    }

    public enum Category {
        SAMPLE,
        LAB_METRIC_RUN
    }

    public enum Key {
        GENDER(Category.SAMPLE),
        PATIENT_ID(Category.SAMPLE),
        SAMPLE_TYPE(Category.SAMPLE),
        TUMOR_NORMAL(Category.SAMPLE),
        COLLECTION_DATE(Category.SAMPLE),
        SAMPLE_ID(Category.SAMPLE),

        CORRELATION_COEFFICIENT_R2(Category.LAB_METRIC_RUN),
        INSTRUMENT_NAME(Category.LAB_METRIC_RUN),
        INSTRUMENT_SERIAL_NUMBER(Category.LAB_METRIC_RUN);

        private Category category;

        Key(Category category) {
            this.category = category;
        }

        public Category getCategory() {
            return category;
        }
    }
}
