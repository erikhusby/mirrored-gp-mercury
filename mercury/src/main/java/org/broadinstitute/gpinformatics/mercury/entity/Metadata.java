package org.broadinstitute.gpinformatics.mercury.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
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

    public static Metadata createMetadata(Key key, String stringValue) {
        switch (key.getDataType()) {
        case STRING:
            return new Metadata(key, stringValue);
        case NUMBER:
            return new Metadata(key, new BigDecimal(stringValue));
        }
        throw new RuntimeException("Unhandled data type " + key.getDataType());
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
        if (key != null) {
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
        }
        return hashCodeBuilder.hashCode();
    }

    public enum DataType {
        STRING,
        NUMBER,
        DATE,
        BOOLEAN
    }

    public enum Category {
        LIQUID_HANDLER_METRIC,
        SAMPLE,
        LAB_METRIC_RUN,
        LAB_METRIC,
        REAGENT
    }

    public enum Visibility {
        USER,
        SYSTEM,
        NONE
    }

    public enum YesNoUnknown implements Displayable {
        YES("Yes"),
        NO("No"),
        UNKNOWN("Unknown");

        private final String displayName;

        YesNoUnknown(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * This enum is part of an external API and should not be changed.
     */
    public enum Key implements Displayable {

        /**
         * The collaborator-assigned sample ID.
         */
        SAMPLE_ID(Category.SAMPLE, DataType.STRING, "Sample ID", Visibility.USER),

        /**
         * The Broad-assigned sample ID (e.g., an SM- ID).
         */
        BROAD_SAMPLE_ID(Category.SAMPLE, DataType.STRING, "Broad Sample ID", Visibility.SYSTEM),

        /**
         * The manufacturer barcode on the sample's container.
         */
        BROAD_2D_BARCODE(Category.SAMPLE, DataType.STRING, "2D Barcode", Visibility.SYSTEM),

        /**
         * The sample's material type, e.g. DNA, Blood, FFPE, etc.
         */
        MATERIAL_TYPE(Category.SAMPLE, DataType.STRING, "Material Type", Visibility.USER),

        /**
         * The type of material from which the sample was derived.
         */
        ORIGINAL_MATERIAL_TYPE(Category.SAMPLE, DataType.STRING, "Original Material Type", Visibility.USER),

        /**
         * A unique ID for the patient. Should not contain PHI.
         */
        PATIENT_ID(Category.SAMPLE, DataType.STRING, "Patient ID", Visibility.USER),

        /**
         * The gender of the patient.
         */
        GENDER(Category.SAMPLE, DataType.STRING, "Gender", Visibility.USER),

        /**
         * The type of sample, tumor or normal. Sometimes called "Sample Type" in other systems.
         */
        TUMOR_NORMAL(Category.SAMPLE, DataType.STRING, "Tumor/Normal", Visibility.USER),

        /**
         * The type of tumor, primary or secondary.
         */
        TUMOR_TYPE(Category.SAMPLE, DataType.STRING, "Tumor Type (Primary, Secondary)", Visibility.USER),

        /**
         * The estimated % of tumor in the sample.
         */
        PERCENT_TUMOR(Category.SAMPLE, DataType.STRING, "Estimated % Tumor", Visibility.USER),

        /**
         * The date that the sample was collected.
         */
        COLLECTION_DATE(Category.SAMPLE, DataType.STRING, "Collection Date", Visibility.USER), // DataType.DATE?

        /**
         * The date that the sample was shipped to Broad.
         */
        SHIPMENT_DATE(Category.SAMPLE, DataType.STRING, "Shipment Date", Visibility.USER), // DataType.DATE?

        /**
         * BUICK_COLLECTION_DATE is stored separately from COLLECTION_DATE because we intend to not use this data for
         * any reason except to pass-through to the final report. Since this is something that the collaborator asked us
         * to store and repeat back, there is no guarantee that it has the same meaning as our own COLLECTION_DATE.
         */
        BUICK_COLLECTION_DATE(Category.SAMPLE, DataType.STRING, "Buick Collection Date", Visibility.USER),
        BUICK_VISIT(Category.SAMPLE, DataType.STRING, "Visit", Visibility.USER),
        RECEIPT_RECORD(Category.SAMPLE, DataType.STRING, "Receipt Record", Visibility.NONE),
        LSID(Category.SAMPLE, DataType.STRING, "Life Science Identifier", Visibility.USER),
        SPECIES(Category.SAMPLE, DataType.STRING, "Species", Visibility.USER),
        BROAD_PARTICIPANT_ID(Category.SAMPLE, DataType.STRING, "Broad Participant ID", Visibility.USER),

        CORRELATION_COEFFICIENT_R2(Category.LAB_METRIC_RUN, DataType.STRING, "R Squared Correlation Coefficient",
                Visibility.USER),
        INSTRUMENT_NAME(Category.LAB_METRIC_RUN, DataType.STRING, "Instrument Name", Visibility.USER),
        INSTRUMENT_SERIAL_NUMBER(Category.LAB_METRIC_RUN, DataType.STRING, "Serial Number", Visibility.USER),

        TOTAL_NG(Category.LAB_METRIC, DataType.NUMBER, "Total ng", Visibility.USER),
        DV_200(Category.LAB_METRIC, DataType.NUMBER, "DV200", Visibility.USER),
        LOWER_MARKER_TIME(Category.LAB_METRIC, DataType.NUMBER, "Lower Marker Time", Visibility.USER),
        NA(Category.LAB_METRIC, DataType.STRING, "NA", Visibility.USER),
        FLOWRATE(Category.LIQUID_HANDLER_METRIC, DataType.NUMBER, "Flowrate", Visibility.USER),
        BAIT_WELL(Category.REAGENT, DataType.STRING, "Bait Well", Visibility.USER),
        DEPLETE_WELL(Category.SAMPLE, DataType.STRING, "Deplete Well", Visibility.USER),
        CELL_TYPE(Category.SAMPLE, DataType.STRING, "Cell Type", Visibility.USER),
        CELLS_PER_WELL(Category.SAMPLE, DataType.NUMBER, "Cells Per Well", Visibility.USER),
        POSITIVE_CONTROL(Category.SAMPLE, DataType.STRING, "Positive Control", Visibility.USER),
        NEGATIVE_CONTROL(Category.SAMPLE, DataType.STRING, "Negative Control", Visibility.USER),
        ROOT_SAMPLE(Category.SAMPLE, DataType.STRING, "Root Sample", Visibility.USER);

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

        public Visibility getVisibility() {
            return visibility;
        }

        public String getValueFor(Sample sample) {
            for (SampleData sampleData : sample.getSampleData()) {
                if (sampleData.getName().equals(name())) {
                    return sampleData.getValue();
                }
            }
            return null;
        }

        public static Key fromDisplayName(String displayName) {

            // todo jmt improve
            Key foundKey = null;

            for (Key key : values()) {
                if (key.getDisplayName().equals(displayName)) {
                    foundKey = key;
                    break;
                }
            }
            return foundKey;
        }
    }
}
