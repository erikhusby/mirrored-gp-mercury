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
    public static final String CREDIT_REQUESTED_FOR = "CRF";
    private final LedgerEntry ledgerEntry;
    private List<LineItem> returnLines;

    BillingCredit(LedgerEntry ledgerEntry, List<LineItem> returnLines) {
        this.ledgerEntry = ledgerEntry;
        this.returnLines = returnLines;
    }

    private LedgerEntry getLedgerEntry() {
        return ledgerEntry;
    }

    /**
     * Create a Collection of BillingCredit objects based on the quoteImport Item. Errors or exceptions are collected
     * for later processing in order to prevent a single credit error halting the entire billing session.
     *
     * @return A Collection of BillingCredits. Only one BillingCredit Object will be returned per sapDeliveryDocument.
     */
    public static Collection<BillingCredit> setupSapCredit(QuoteImportItem quoteItem) {
        Set<BillingCredit> credits = new HashSet<>();
        double totalAvailable = quoteItem.getPriorBillings().stream().mapToDouble(LedgerEntry::getQuantity).sum();
        double quoteItemQuantity = quoteItem.getQuantity();
        if (quoteItemQuantity > 0) {
            throw new BillingException(BillingAdaptor.CREDIT_QUANTITY_INVALID);
        } if (totalAvailable + quoteItemQuantity < 0 ){
            throw new BillingException(BillingAdaptor.NEGATIVE_BILL_ERROR);
        } else {
            Map<LedgerEntry, List<LineItem>> orderItemsByDeliveryDocument = new HashMap<>();
            quoteItem.getBillingCredits().forEach(ledgerCreditEntry -> {
                Map<LedgerEntry, Double> creditSource = ledgerCreditEntry.findCreditSource();
                creditSource.forEach((ledgerEntry, quantityAvailableInCreditSource) -> {
                    BillingMessage billingMessage=new BillingMessage();
                    if (creditSource.isEmpty() && Math.abs(quoteItemQuantity) > 0) {
                        billingMessage.setValidationError(BillingAdaptor.NEGATIVE_BILL_ERROR);
                    }

                    Double totalQuantitiesAvailableInLedger =
                        ledgerEntry.getPreviouslyBilled().stream().mapToDouble(LedgerEntry::getQuantity).sum();

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
                    orderItemsByDeliveryDocument.computeIfAbsent(ledgerEntry, k -> new ArrayList<>())
                        .add(new LineItem(quoteItem.getProduct().getPartNumber(), requestedCreditQuantity, billingMessage));
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

    public String getReturnOrderId() {
        return getLedgerEntry().getSapReturnOrderId();
    }

    public String getSapDeliveryDocumentId() {
        return getLedgerEntry().getSapDeliveryDocumentId();
    }

    public void setSapDeliveryDocumentId(String sapDeliveryDocumentId) {
        getLedgerEntry().setSapDeliveryDocumentId(sapDeliveryDocumentId);
    }

    public void setReturnOrderId(String returnOrderId) {
        getLedgerEntry().setSapReturnOrderId(returnOrderId);
    }

    public void setReturnOrderInvoiceNotFound() {
        setReturnOrderId(String.format("%s%s", CREDIT_REQUESTED_FOR, getLedgerEntry().getSapDeliveryDocumentId()));
    }

    public static class LineItem {
        private final String partNumber;
        private final BigDecimal quantity;
        private BillingMessage billingMessage;

        LineItem(String partNumber, Double quantity, BillingMessage billingMessage) {
            this.partNumber = partNumber;
            this.quantity = BigDecimal.valueOf(quantity);
            this.billingMessage = billingMessage;
        }

        public BillingMessage getBillingMessage() {
            return billingMessage;
        }

        public SAPOrderItem getSapOrderItem() {
            return new SAPOrderItem(partNumber, quantity);
        }
    }

//    public static class BillingCreditMessage {
//        private final String sapDeliveryDocument;
//
//        private final QuoteImportItem quoteImportItem;
//        private final SAPProductPriceCache productPriceCache;
//        private final Long orderCreatedBy;
//        private String mercuryOrder;
//        private String material;
//        private String sapOrderNumber;
//        private String deliveryDiscount;
//
//        public BillingCreditMessage(QuoteImportItem quoteImportItem, SAPProductPriceCache productPriceCache) {
//            this.quoteImportItem = quoteImportItem;
//            this.productPriceCache = productPriceCache;
//
//            this.mercuryOrder = quoteImportItem.getProductOrder().getJiraTicketKey();
//            this.material = quoteImportItem.getProduct().getDisplayName();
//            this.sapOrderNumber = quoteImportItem.getProductOrder().getSapOrderNumber();
//            this.orderCreatedBy = quoteImportItem.getProductOrder().getCreatedBy();
//            this.sapDeliveryDocument = Stream.concat(
//                quoteImportItem.getPriorBillings().stream().map(LedgerEntry::getSapDeliveryDocumentId),
//                quoteImportItem.getPriorBillings().stream().map(LedgerEntry::getSapReturnOrderId)
//            ).filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("<br/>"));
//            this.deliveryDiscount = buildDeliveryDiscount();
//        }
//
//
//        public Long getOrderCreatedBy() {
//            return orderCreatedBy;
//        }
//
//        public String getSapDeliveryDocument() {
//            return sapDeliveryDocument;
//        }
//
//        public String getMercuryOrder() {
//            return mercuryOrder;
//        }
//
//        public String getMaterial() {
//            return material;
//        }
//
//        public String getDeliveryDiscount() {
//            return deliveryDiscount;
//        }
//
//        public String getSapOrderNumber() {
//            return sapOrderNumber;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) {
//                return true;
//            }
//
//            if (o == null || getClass() != o.getClass()) {
//                return false;
//            }
//
//            BillingCreditMessage that = (BillingCreditMessage) o;
//
//            return new EqualsBuilder()
//                .append(getMercuryOrder(), that.getMercuryOrder())
//                .append(getMaterial(), that.getMaterial())
//                .append(getSapOrderNumber(), that.getSapOrderNumber())
//                .append(getSapDeliveryDocument(), that.getSapDeliveryDocument())
//                .append(getDeliveryDiscount(), that.getDeliveryDiscount())
//                .isEquals();
//        }
//
//        @Override
//        public int hashCode() {
//            return new HashCodeBuilder(17, 37)
//                .append(getMercuryOrder())
//                .append(getMaterial())
//                .append(getSapOrderNumber())
//                .append(getSapDeliveryDocument())
//                .append(getDeliveryDiscount())
//                .toHashCode();
//        }
//    }
}
