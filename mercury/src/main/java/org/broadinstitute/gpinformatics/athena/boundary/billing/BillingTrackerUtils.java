package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import java.util.*;

public class BillingTrackerUtils {

    private static final Log logger = LogFactory.getLog(BillingTrackerUtils.class);

    public static final String[] FIXED_HEADERS = {
            BillingTrackerHeader.SAMPLE_ID_HEADING.getText(),
            BillingTrackerHeader.COLLABORATOR_SAMPLE_ID.getText(),
            BillingTrackerHeader.MATERIAL_TYPE.getText(),
            BillingTrackerHeader.ON_RISK.getText(),
            BillingTrackerHeader.STATUS.getText(),
            BillingTrackerHeader.PRODUCT_NAME.getText(),
            BillingTrackerHeader.ORDER_ID_HEADING.getText(),
            BillingTrackerHeader.PRODUCT_ORDER_NAME.getText(),
            BillingTrackerHeader.PROJECT_MANAGER.getText(),
            BillingTrackerHeader.AUTO_LEDGER_TIMESTAMP_HEADING.getText(),
            BillingTrackerHeader.WORK_COMPLETE_DATE_HEADING.getText(),
            BillingTrackerHeader.QUOTE_ID_HEADING.getText(),
            BillingTrackerHeader.SORT_COLUMN_HEADING.getText()
    };

    private static RuntimeException getRuntimeException(String errMsg) {
        logger.error(errMsg);
        return new RuntimeException(errMsg);
    }

    static BillableRef extractBillableRefFromHeader(String cellValueStr) {

        if (StringUtils.isBlank(cellValueStr)) {
            throw new NullPointerException("Header name cannot be blank");
        }

        int endPos = cellValueStr.lastIndexOf("]");
        int startPos = cellValueStr.lastIndexOf("[", endPos);
        if ((endPos < 0) || (startPos < 0) || !(startPos + 1 < endPos)) {
            throw getRuntimeException(
                "Tracker Sheet Header Format Error.  Could not find product partNumber in " +
                "column header. Column header contained: <" +  cellValueStr + ">");
        }

        String productPartNumber = cellValueStr.substring(startPos + 1, endPos);
        // Substring from char position 0 to position before separating space char.
        String priceItemName = cellValueStr.substring(0, startPos - 1);
        return new BillableRef(productPartNumber, priceItemName);
    }
}
