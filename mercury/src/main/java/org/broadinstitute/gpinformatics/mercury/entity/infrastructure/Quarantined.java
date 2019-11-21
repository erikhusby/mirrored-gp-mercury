package org.broadinstitute.gpinformatics.mercury.entity.infrastructure;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Arrays;
import java.util.List;

@Audited
@Entity
@Table(schema = "mercury", name = "QUARANTINED", uniqueConstraints = @UniqueConstraint(columnNames = {"ITEM"}))
public class Quarantined {
    // Non-user selectable quarantine reasons.
    public static final String MISMATCH = "Wrong tube or position";
    public static final String H12_G12 = "Tube at H12 or G12";
    // The next one is used in gpuitest. If you change it here, change it there too.
    public static final String MISSING_MANIFEST = "Missing manifest";
    public static final String RACK_BARCODE_MISMATCH = "Entered rack barcode(s) don't match manifest";
    // User selectable quarantine reasons.
    private static final List<String> RACK_REASONS = Arrays.asList(
            "Unreadable barcode",
            "Damaged"
    );

    public enum ItemSource {MAYO};
    public enum ItemType {PACKAGE, RACK};

    @Id
    @SequenceGenerator(name = "SEQ_QUARANTINED", schema = "mercury", sequenceName = "SEQ_QUARANTINED")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_QUARANTINED")
    private Long quarantinedId;

    @Column
    private ItemSource itemSource;

    @Column
    private ItemType itemType;

    /** This identifies the quarantined item. */
    @Column(length = 255)
    private String item;

    @Column(length = 255)
    private String reason;

    public Quarantined() {
    }

    public Quarantined(ItemSource itemSource, ItemType itemType, String item, String reason) {
        this.itemSource = itemSource;
        this.itemType = itemType;
        this.item = item;
        this.reason = reason;
    }

    public Long getQuarantinedId() {
        return quarantinedId;
    }

    public ItemSource getItemSource() {
        return itemSource;
    }

    public void setItemSource(ItemSource itemSource) {
        this.itemSource = itemSource;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static List<String> getRackReasons() {
        return RACK_REASONS;
    }
}
