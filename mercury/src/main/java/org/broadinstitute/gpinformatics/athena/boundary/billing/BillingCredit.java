/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.sap.entity.order.SAPOrderItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BillingCredit {
    public static final String CREDIT_REQUESTED = "CR-";
    private String sapDeliveryDocumentId;
    private List<LineItem> returnLines;
    private String returnOrderId;
    private BillingMessage billingMessage = new BillingMessage();
    private BillingEjb.BillingResult billingResult;

    BillingCredit(String sapDeliveryDocumentId, List<LineItem> returnLines) {
        this.sapDeliveryDocumentId = sapDeliveryDocumentId;
        this.returnLines = returnLines;
    }

    public BillingMessage getBillingMessage() {
        return billingMessage;
    }

    public void setBillingMessage(BillingMessage billingMessage) {
        this.billingMessage = billingMessage;
    }

    /**
     * Create a Collection of BillingCredit objects based on the quoteImport Item. Errors or exceptions are collected
     * for later processing in order to prevent a single credit error halting the entire billing session.
     *
     * @return A Collection of BillingCredits. Only one BillingCredit Object will be returned per sapDeliveryDocument.
     */
    public static Collection<BillingCredit> setupSapCredits(QuoteImportItem quoteItem) {
        Set<BillingCredit> credits = new HashSet<>();
        double totalAvailable = quoteItem.getPriorBillings().stream().mapToDouble(LedgerEntry::getQuantity).sum();
        double quoteItemQuantity = quoteItem.getQuantity();
        if (quoteItemQuantity > 0) {
            throw new BillingException(BillingAdaptor.CREDIT_QUANTITY_INVALID);
        } if (totalAvailable + quoteItemQuantity < 0 ){
            throw new BillingException(BillingAdaptor.NEGATIVE_BILL_ERROR);
        } else {
            Map<String, List<LineItem>> orderItemsByDeliveryDocument = new HashMap<>();
            quoteItem.getBillingCredits().forEach(ledgerCreditEntry -> {
                Map<LedgerEntry, Double> creditSource = ledgerCreditEntry.findCreditSource();
                creditSource.forEach((sourceLedger, quantityAvailableInCreditSource) -> {
                    BillingMessage billingMessage=new BillingMessage();
                    if (creditSource.isEmpty() && Math.abs(quoteItemQuantity) > 0) {
                        billingMessage.setValidationError(BillingAdaptor.NEGATIVE_BILL_ERROR);
                    }

                    Double totalQuantitiesAvailableInLedger =
                        sourceLedger.getPreviouslyBilled().stream().mapToDouble(LedgerEntry::getQuantity).sum();

                    if (ledgerCreditEntry.getQuantity() >= 0) {
                        billingMessage.setValidationError(BillingAdaptor.CREDIT_QUANTITY_INVALID);
                        return;
                    }
                    Double requiredCreditQuantity = Math.abs(ledgerCreditEntry.getQuantity());
                    Double requestedCreditQuantity = 0d;
                    if (requiredCreditQuantity > quantityAvailableInCreditSource
                        && requiredCreditQuantity > totalQuantitiesAvailableInLedger) {
                        billingMessage.setValidationError(BillingAdaptor.NEGATIVE_BILL_ERROR);
                        return;
                    } else if (requiredCreditQuantity.equals(quantityAvailableInCreditSource)) {
                        requestedCreditQuantity = quantityAvailableInCreditSource;
                    } else {
                        requestedCreditQuantity = requiredCreditQuantity;
                    }
                    orderItemsByDeliveryDocument.computeIfAbsent(sourceLedger.getSapDeliveryDocumentId(), k -> new ArrayList<>())
                        .add(new LineItem(ledgerCreditEntry, requestedCreditQuantity, billingMessage));
                });
            });

            orderItemsByDeliveryDocument.forEach((deliveryDocument, creditItems) -> {
                credits.add(new BillingCredit(deliveryDocument, creditItems));
            });
        }
        return credits;
    }

    public List<LineItem> getReturnLines() {
        return returnLines;
    }

    public String getSapDeliveryDocumentId() {
        return sapDeliveryDocumentId;
    }

    public void setReturnOrderId(String returnOrderId) {
        this.returnOrderId = returnOrderId;
    }

    public String getReturnOrderId() {
        return returnOrderId;
    }

    public void setReturnOrderInvoiceNotFound() {
        String sapDeliveryDocumentId = getSapDeliveryDocumentId();
        if (StringUtils.isNotBlank(sapDeliveryDocumentId)) {
            setReturnOrderId(String.format("%s%s", CREDIT_REQUESTED, sapDeliveryDocumentId));
        }
    }

    public void setBillingResult(BillingEjb.BillingResult billingResult) {
        this.billingResult = billingResult;
    }

    public BillingEjb.BillingResult getBillingResult() {
        return billingResult;
    }

    public void setSapDeliveryDocumentId(String sapDeliveryDocumentId) {
        this.sapDeliveryDocumentId = sapDeliveryDocumentId;
    }

    public static class LineItem {
        private final LedgerEntry ledgerEntry;
        private final BigDecimal quantity;
        private BillingMessage billingMessage;

        LineItem(LedgerEntry ledgerEntry, Double quantity, BillingMessage billingMessage) {
            this.ledgerEntry = ledgerEntry;
            this.quantity = BigDecimal.valueOf(quantity);
            this.billingMessage = billingMessage;
        }

        public LedgerEntry getLedgerEntry() {
            return ledgerEntry;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public BillingMessage getBillingMessage() {
            return billingMessage;
        }

        public SAPOrderItem getSapOrderItem() {
            return new SAPOrderItem(ledgerEntry.getProduct().getPartNumber(), quantity);
        }
    }

}
