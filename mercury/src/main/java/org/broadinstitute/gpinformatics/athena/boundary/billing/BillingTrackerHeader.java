package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;

/**
 * This enum holds the headers for the billing tracker and provides access to the headers for parsing spreadsheets.
 */
public enum BillingTrackerHeader implements ColumnHeader {

    SAMPLE_ID_HEADING("Sample ID", 0, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    COLLABORATOR_SAMPLE_ID("Collaborator Sample ID", 1, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    MATERIAL_TYPE("Material Type", 2, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    ON_RISK("On Risk", 3, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    STATUS("Status", 4, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    PRODUCT_NAME("Product Name", 5, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    ORDER_ID_HEADING("Product Order ID", 6, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE),
    PRODUCT_ORDER_NAME("Product Order Name", 7, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    PROJECT_MANAGER("Project Manager", 8, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    AUTO_LEDGER_TIMESTAMP_HEADING("Auto Ledger Timestamp", 9, ColumnHeader.OPTIONAL_HEADER, ColumnHeader.OPTIONAL_HEADER),
    WORK_COMPLETE_DATE_HEADING("Date Completed", 10, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_HEADER),
    QUOTE_ID_HEADING("Quote ID", 11, ColumnHeader.REQUIRED_HEADER, ColumnHeader.OPTIONAL_HEADER),
    SORT_COLUMN_HEADING("Sort Column", 12, ColumnHeader.REQUIRED_HEADER, ColumnHeader.REQUIRED_VALUE);

    public static final String BILLED = "Billed";
    public static final String UPDATE = "Update Quantity To";

    private final String text;
    private final int index;
    private final boolean requredHeader;
    private final boolean requiredValue;

    private BillingTrackerHeader(String text, int index, boolean requiredHeader, boolean requiredValue) {
        this.text = text;
        this.index = index;
        this.requredHeader = requiredHeader;
        this.requiredValue = requiredValue;
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
    public boolean isRequredHeader() {
        return requredHeader;
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
}
