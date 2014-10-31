package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

/**
 * This enum holds the headers for the billing tracker and provides access to the headers for parsing spreadsheets.
 */
public enum BillingTrackerHeader implements ColumnHeader {

    SAMPLE_ID("Sample ID", ColumnHeader.REQUIRED_HEADER),
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", ColumnHeader.OPTIONAL_HEADER),
    MATERIAL_TYPE("Material Type", ColumnHeader.OPTIONAL_HEADER),
    ON_RISK("On Risk", ColumnHeader.OPTIONAL_HEADER),
    STATUS("Status", ColumnHeader.OPTIONAL_HEADER),
    PRODUCT_NAME("Product Name", ColumnHeader.OPTIONAL_HEADER),
    ORDER_ID("Product Order ID", ColumnHeader.REQUIRED_HEADER),
    PRODUCT_ORDER_NAME("Product Order Name", ColumnHeader.OPTIONAL_HEADER),
    PROJECT_MANAGER("Project Manager", ColumnHeader.OPTIONAL_HEADER),
    LANE_COUNT("Lane Count", ColumnHeader.OPTIONAL_HEADER) {
        @Override
        public boolean shouldShow(Product product) {
            return product.getSupportsNumberOfLanes();
        }
    },
    AUTO_LEDGER_TIMESTAMP("Auto Ledger Timestamp", ColumnHeader.OPTIONAL_HEADER, IsDate.YES),
    WORK_COMPLETE_DATE("Date Completed", ColumnHeader.REQUIRED_HEADER, IsDate.YES),
    PF_READS("PF Reads", ColumnHeader.OPTIONAL_HEADER),
    PF_ALIGNED_GB("PF Aligned GB", ColumnHeader.OPTIONAL_HEADER),
    PF_READS_ALIGNED_IN_PAIRS("PF Reads Aligned in Pairs", ColumnHeader.OPTIONAL_HEADER),
    PERCENT_COVERAGE_AT_20X("% Coverage at 20X", ColumnHeader.OPTIONAL_HEADER) {
        @Override public boolean shouldShow(Product product) {
            return product.isSameProductFamily(ProductFamily.ProductFamilyName.EXOME);
        }
    },
    TARGET_BASES_100X_PERCENT("Target Bases 100x %", ColumnHeader.OPTIONAL_HEADER) {
        @Override public boolean shouldShow(Product product) {
            return product.isSameProductFamily(ProductFamily.ProductFamilyName.EXOME);
        }
    },
    SAMPLE_TYPE("Sample Type", ColumnHeader.OPTIONAL_HEADER),
    TABLEAU_LINK("Tableau", ColumnHeader.OPTIONAL_HEADER),
    QUOTE_ID("Quote ID", ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_VALUE),
    SORT_COLUMN("Sort Column", ColumnHeader.REQUIRED_HEADER);

    private enum IsDate {
        YES, NO
    }

    public static final String BILLED = "Billed";
    public static final String UPDATE = "Update Quantity To";

    private final String text;
    private final boolean requiredHeader;
    private final boolean requiredValue;
    private final IsDate isDate;

    /**
     * Construct a header where the 'required header' and 'required value' are the same. This is the most common case.
     */
    BillingTrackerHeader(String text, boolean requiredHeader) {
        this(text, requiredHeader, IsDate.NO);
    }

    /**
     * Construct a header where the 'required header' and 'required value' are the same. This is the most common case.
     */
    BillingTrackerHeader(String text, boolean requiredHeader, IsDate isDate) {
        this(text, requiredHeader, requiredHeader, isDate);
    }

    BillingTrackerHeader(String text, boolean requiredHeader, boolean requiredValue) {
        this(text, requiredHeader, requiredValue, IsDate.NO);
    }

    BillingTrackerHeader(String text, boolean requiredHeader, boolean requiredValue, IsDate isDate) {
        this.text = text;
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
        return text;
    }

    @Override
    public boolean isRequiredHeader() {
        return requiredHeader;
    }

    @Override
    public boolean isRequiredValue() {
        return requiredValue;
    }

    /**
     * Generates a header string for the price item name, which includes the "Billed" column header. Used when creating
     * the billing tracker download.
     *
     * @param priceItem    the price item to get the name from
     * @return the header text
     */
    public static String getPriceItemNameHeader(PriceItem priceItem) {
        return priceItem.getName() + "\n" + BILLED;
    }

    /**
     * Generates a header string for the price item name, which includes the "Billed" column header. Used when creating
     * the billing tracker download.
     *
     * @param priceItem    the price item to get the name from
     * @return the header text
     */
    public static String getHistoricalPriceItemNameHeader(PriceItem priceItem) {
        return priceItem.getName() + "\n" + BILLED + " (Historical)";
    }

    /**
     * Generates a header string for the product part number, which includes the "Update Quantity To" column header.
     * Used when creating the billing tracker download.
     *
     * @param product    the product to get the part number from
     * @return the header text
     */
    public static String getPriceItemPartNumberHeader(Product product) {
        return getPartNumberHeader(product.getPartNumber());
    }

    /**
     * Generates a header string for the part number for the given billable product. Used when parsing an uploaded
     * billing tracker.
     *
     *
     * @param priceItem      th price item to get the name from
     * @param billableRef    the billable product to get the part number from
     * @return the header text
     */
    public static String getPriceItemPartNumberHeader(PriceItem priceItem, BillableRef billableRef) {
        return getPriceItemNameHeader(priceItem) + " " + getPartNumberHeader(billableRef.getProductPartNumber());
    }

    /**
     * Generates a header string for the given part number.
     *
     * @param partNumber    the part number
     * @return the header text
     */
    private static String getPartNumberHeader(String partNumber) {
        return "[" + partNumber + "]\n" + UPDATE;
    }

    @Override
    public boolean isStringColumn() {
        return false;
    }

    @Override
    public boolean isDateColumn() {
        return isDate == IsDate.YES;
    }
}
