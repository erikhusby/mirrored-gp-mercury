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
    public static final String METADATA_KEY_NOT_FOUND = "No metadata key found with name %s";

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

        /**
         * Sample data stored in Mercury. Typically imported from some sort of manifest spreadsheet, e.g., the "Buick"
         * manifest or the CRSP sample data spreadsheet (via web service call from the CRSP Portal).
         */
        SAMPLE,
        LAB_METRIC_RUN,
        LAB_METRIC
    }

    /**
     * Metadata keys for storing sample data. The data stored under each key should have consistent meaning across all
     * samples containing data for that key.
     */
    public enum Key implements Displayable {

        /**
         * The collaborator-assigned sample ID.
         */
        SAMPLE_ID(Category.SAMPLE, DataType.STRING, "Sample ID"),

        /**
         * The Broad-assigned sample ID (e.g., an SM- ID).
         */
        BROAD_SAMPLE_ID(Category.SAMPLE, DataType.STRING, "Broad Sample ID"),

        /**
         * The sample's material type, e.g. DNA, Blood, FFPE, etc.
         */
        MATERIAL_TYPE(Category.SAMPLE, DataType.STRING, "Material Type"),

        /**
         * The type of material from which the sample was derived.
         */
        ORIGINAL_MATERIAL_TYPE(Category.SAMPLE, DataType.STRING, "Original Material Type"),

        /**
         * A unique ID for the patient. Should not contain PHI.
         */
        PATIENT_ID(Category.SAMPLE, DataType.STRING, "Patient ID"),

        /**
         * The gender of the patient.
         */
        GENDER(Category.SAMPLE, DataType.STRING, "Gender"),

        /**
         * The type of sample, tumor or normal. Sometimes called "Sample Type" in other systems.
         */
        TUMOR_NORMAL(Category.SAMPLE, DataType.STRING, "Tumor/Normal"),

        /**
         * The estimated % of tumor in the sample.
         */
        PERCENT_TUMOR(Category.SAMPLE, DataType.STRING, "Estimated % Tumor"),

        /**
         * The date that the sample was collected.
         */
        COLLECTION_DATE(Category.SAMPLE, DataType.STRING, "Collection Date"), // DataType.DATE?

        /**
         * The date that the sample was shipped to Broad.
         */
        SHIPMENT_DATE(Category.SAMPLE, DataType.STRING, "Shipment Date"), // DataType.DATE?

        /**
         * BUICK_COLLECTION_DATE is stored separately from COLLECTION_DATE because we intend to not use this data for
         * any reason except to pass-through to the final report. Since this is something that the collaborator asked us
         * to store and repeat back, there is no guarantee that it has the same meaning as our own COLLECTION_DATE.
         */
        BUICK_COLLECTION_DATE(Category.SAMPLE, DataType.STRING, "Collection Date"),
        BUICK_VISIT(Category.SAMPLE, DataType.STRING, "Visit"),

        CORRELATION_COEFFICIENT_R2(Category.LAB_METRIC_RUN, DataType.STRING, "R Squared Correlation Coefficient"),
        INSTRUMENT_NAME(Category.LAB_METRIC_RUN, DataType.STRING, "Instrument Name"),
        INSTRUMENT_SERIAL_NUMBER(Category.LAB_METRIC_RUN, DataType.STRING, "Serial Number"),

        TOTAL_NG(Category.LAB_METRIC, DataType.NUMBER, "Total ng");

        private final Category category;
        private final DataType dataType;
        private final String displayName;

        Key(Category category, DataType dataType, String displayName) {
            this.category = category;
            this.dataType = dataType;
            this.displayName = displayName;
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

        public static Metadata.Key fromDisplayName(String displayName) {
            for (Key key : Key.values()) {
                if (key.getDisplayName().equals(displayName)) {
                    return key;
                }
            }
            throw new IllegalArgumentException(String.format(METADATA_KEY_NOT_FOUND, displayName));
        }

    }
}
