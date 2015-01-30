package org.broadinstitute.gpinformatics.mercury.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
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
import java.math.BigDecimal;
import java.text.Format;
import java.util.Date;

/**
 * Generic metadata storage class with String keys and values.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "METADATA")
public class Metadata {
    public static final Format DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd-HH:mm:ss");

    @Id
    @SequenceGenerator(name = "SEQ_METADATA", schema = "mercury", sequenceName = "SEQ_METADATA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_METADATA")
    /** ID field for JPA */
    @SuppressWarnings("UnusedDeclaration")
    @Column(name = "metadata_id")
    private Long id;

    @Column(name = "key")
    @Enumerated(EnumType.STRING)
    private Key key;

    @Column(name = "value")
    private String stringValue;

    @Column(name = "number_value")
    private BigDecimal numberValue;

    @Column(name = "date_value")
    private Date dateValue;

    /**
     * For JPA
     */
    protected Metadata() {
    }

    public Metadata(@Nonnull Key key, String stringValue) {
        this.key = key;
        this.stringValue = stringValue;
        if (key.getDataType() != DataType.STRING) {
            throw new RuntimeException("String value passed to " + key.toString());
        }
    }

    public Metadata(@Nonnull Key key, BigDecimal numberValue) {
        this.key = key;
        this.numberValue = numberValue;
        if (key.getDataType() != DataType.NUMBER) {
            throw new RuntimeException("Number value passed to " + key.toString());
        }
    }

    public Metadata(@Nonnull Key key, Date dateValue) {
        this.key = key;
        this.dateValue = dateValue;
        if (key.getDataType() != DataType.DATE) {
            throw new RuntimeException("Date value passed to " + key.toString());
        }
    }

    @Nonnull
    public Key getKey() {
        return key;
    }

    public String getValue() {
        switch (key.getDataType()) {
        case STRING:
            return stringValue;
        case NUMBER:
            return numberValue.toString();
        case DATE:
            return DATE_FORMAT.format(dateValue);
        }
        throw new RuntimeException("Unhandled data type " + key.getDataType());
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public BigDecimal getNumberValue() {
        return numberValue;
    }

    public Date getDateValue() {
        return dateValue;
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

        EqualsBuilder equalsBuilder = new EqualsBuilder().append(key, metadata.getKey());
        switch (key.getDataType()) {
        case STRING:
            equalsBuilder.append(stringValue, metadata.getStringValue());
            break;
        case NUMBER:
            equalsBuilder.append(numberValue, metadata.getNumberValue());
            break;
        case DATE:
            equalsBuilder.append(dateValue, metadata.getDateValue());
            break;
        }
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder().append(key);
        switch (key.getDataType()) {
        case STRING:
            hashCodeBuilder.append(stringValue);
            break;
        case NUMBER:
            hashCodeBuilder.append(numberValue);
            break;
        case DATE:
            hashCodeBuilder.append(dateValue);
            break;
        }
        return hashCodeBuilder.hashCode();
    }

    public enum DataType {
        STRING,
        NUMBER,
        DATE
    }

    public enum Category {
        SAMPLE,
        LAB_METRIC_RUN,
        LAB_METRIC
    }

    public enum Visibility {
        USER,
        SYSTEM,
        NONE
    }

    public enum Key implements Displayable {
        // The Category.SAMPLE keys are currently all used for uploads of the "modified" (edited) manifest during
        // Buick sample registration.
        GENDER(Category.SAMPLE, DataType.STRING, "Gender", Visibility.USER),
        PATIENT_ID(Category.SAMPLE, DataType.STRING, "Patient ID", Visibility.USER),
        SAMPLE_TYPE(Category.SAMPLE, DataType.STRING, "Sample Type", Visibility.USER),
        TUMOR_NORMAL(Category.SAMPLE, DataType.STRING, "Tumor/Normal", Visibility.USER),
        BUICK_COLLECTION_DATE(Category.SAMPLE, DataType.STRING, "Collection Date", Visibility.USER),
        SAMPLE_ID(Category.SAMPLE, DataType.STRING, "Sample ID", Visibility.USER),
        BUICK_VISIT(Category.SAMPLE, DataType.STRING, "Visit", Visibility.USER),

        CORRELATION_COEFFICIENT_R2(Category.LAB_METRIC_RUN, DataType.STRING, "R Squared Correlation Coefficient",
                Visibility.USER),
        INSTRUMENT_NAME(Category.LAB_METRIC_RUN, DataType.STRING, "Instrument Name", Visibility.USER),
        INSTRUMENT_SERIAL_NUMBER(Category.LAB_METRIC_RUN, DataType.STRING, "Serial Number", Visibility.USER),

        TOTAL_NG(Category.LAB_METRIC, DataType.NUMBER, "Total ng", Visibility.USER),
        BROAD_SAMPLE_ID(Category.SAMPLE, DataType.STRING, "SM ID", Visibility.SYSTEM),
        BROAD_2D_BARCODE(Category.SAMPLE, DataType.STRING, "2D Barcode", Visibility.SYSTEM);

        private final Category category;
        private final DataType dataType;
        private final String displayName;
        private final Visibility visibility;

        Key(Category category, DataType dataType, String displayName, Visibility visibility) {
            this.category = category;
            this.dataType = dataType;
            this.displayName = displayName;
            this.visibility = visibility;
        }

        public Category getCategory() {
            return category;
        }

        public DataType getDataType() {
            return dataType;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
}
