package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

/**
 * This enum holds the headers for the billing tracker and provides access to the headers for parsing spreadsheets.
 */
public enum BillingTrackerHeader implements ColumnHeader {

    SAMPLE_ID("Sample ID", 0, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", 1, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    MATERIAL_TYPE("Material Type", 2, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    ON_RISK("On Risk", 3, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    STATUS("Status", 4, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    PRODUCT_NAME("Product Name", 5, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    ORDER_ID("Product Order ID", 6, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    PRODUCT_ORDER_NAME("Product Order Name", 7, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    PROJECT_MANAGER("Project Manager", 8, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    LANE_COUNT("Lane Count", 9, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER) {
        @Override public boolean shouldShow(Product product) { return product.getSupportsNumberOfLanes(); }
    },
    AUTO_LEDGER_TIMESTAMP("Auto Ledger Timestamp", 10, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER, true),
    WORK_COMPLETE_DATE("Date Completed", 11, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_HEADER, true),
    QUOTE_ID("Quote ID", 12, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_HEADER),
    SORT_COLUMN("Sort Column", 13, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE);

    public static final String BILLED = "Billed";
    public static final String UPDATE = "Update Quantity To";

    private final String text;
    private final int index;
    private final boolean requiredHeader;
    private final boolean requiredValue;
    private final boolean isDate;

    private BillingTrackerHeader(String text, int index, boolean requiredHeader, boolean requiredValue) {
        this(text, index, requiredHeader, requiredValue, false);
    }

    private BillingTrackerHeader(String text, int index, boolean requiredHeader, boolean requiredValue, boolean isDate) {
        this.text = text;
        this.index = index;
        this.requiredHeader = requiredHeader;
        this.requiredValue = requiredValue;
        this.isDate = isDate;
    }

    /**
     * Returns whether or not to show this header/column for the given product. Individual enum values can override this
     * method to allow for conditional display of columns.
     *
     * @param product    the product for the current billing tracker sheet
     * @return true if the column should be displayed; false otherwise
     */
    public boolean shouldShow(Product product) {
        return true;
    }

    @Override
    public String getText() {
        return this.text;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public boolean isRequiredHeader() {
        return requiredHeader;
    }

    @Override
    public boolean isRequiredValue() {
        return requiredValue;
    }

    public static String getPriceItemHeader(PriceItem priceItem, Product product) {
        return priceItem.getName() + " [" + product.getPartNumber() + "]";
    }

    public static String getPriceItemHeader(BillableRef billableRef) {
        return billableRef.getPriceItemName() + " [" + billableRef.getProductPartNumber() + "]";
    }

    @Override
    public boolean isStringColumn() {
        return false;
    }

    @Override
    public boolean isDateColumn() {
        return isDate;
    }
}
