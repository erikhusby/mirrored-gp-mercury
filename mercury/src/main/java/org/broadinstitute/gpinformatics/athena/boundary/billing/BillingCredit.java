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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class BillingCredit {
    public static final String CREDIT_REQUESTED_PREFIX = "CR-";
    private String deliveryDoc;
    private Collection<LineItem> returnLines;
    private String returnOrderId;
    private BillingMessage billingMessage = new BillingMessage();
    private BillingEjb.BillingResult billingResult;

    BillingCredit(String deliveryDoc, Collection<LineItem> returnLines) {
        this.deliveryDoc = deliveryDoc;
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
        BigDecimal totalAvailable = quoteItem.totalPriorBillingQuantity();
        BigDecimal quoteItemQuantity = quoteItem.getQuantity();
        AtomicReference<BigDecimal> requiredCreditQuantity = new AtomicReference<>(quoteItemQuantity.abs());
        if (quoteItemQuantity.compareTo(BigDecimal.ZERO) >=0) {
            throw new BillingException(BillingAdaptor.CREDIT_QUANTITY_INVALID);
        } if (totalAvailable.add(quoteItemQuantity).compareTo(BigDecimal.ZERO) < 0 ){
            throw new BillingException(BillingAdaptor.NEGATIVE_BILL_ERROR);
        } else {
            Map<String, Set<LineItem>> orderItemsByDeliveryDocument = new HashMap<>();
            Collection<LedgerEntry> billingCredits = quoteItem.getBillingCredits();
            billingCredits.forEach(ledgerCreditEntry -> {

                    Map<LedgerEntry, BigDecimal> creditSource = ledgerCreditEntry.findCreditSource();
                    creditSource.forEach((sourceLedger, quantityAvailableInCreditSource) -> {
                        BillingMessage billingMessage = new BillingMessage();
                        if (creditSource.isEmpty() && quoteItemQuantity.abs().compareTo(BigDecimal.ZERO) > 0) {
                            billingMessage.setValidationError(BillingAdaptor.NEGATIVE_BILL_ERROR);
                        }

                        if (ledgerCreditEntry.getQuantity().compareTo(BigDecimal.ZERO) >= 0) {
                            billingMessage.setValidationError(BillingAdaptor.CREDIT_QUANTITY_INVALID);
                            return;
                        }


                        BigDecimal quantityInLineItems = orderItemsByDeliveryDocument.values().stream().map(
                            lineItems -> lineItems.stream().map(LineItem::getQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)).reduce(BigDecimal.ZERO, BigDecimal::add);

                        if (quantityInLineItems.compareTo(quoteItemQuantity.abs()) >= 0) {
                            return;
                        }
                        BigDecimal quantityToCredit =
                            quantityAvailableInCreditSource.compareTo(requiredCreditQuantity.get()) > 0 ?
                                requiredCreditQuantity.get() : quantityAvailableInCreditSource;


                        if (requiredCreditQuantity.get().compareTo(quantityAvailableInCreditSource) > 0
                            && requiredCreditQuantity.get().compareTo(totalAvailable) > 0) {
                            billingMessage.setValidationError(BillingAdaptor.NEGATIVE_BILL_ERROR);
                            return;
                        }
                        requiredCreditQuantity.getAndAccumulate(quantityToCredit, BigDecimal::subtract);
                        Set<LineItem> lineItems = orderItemsByDeliveryDocument
                            .computeIfAbsent(sourceLedger.getSapDeliveryDocumentId(), k -> new HashSet<>());
//                        lineItems.add(new LineItem(sourceLedger, ledgerCreditEntry, quantityToCredit, billingMessage));
                        LineItem.merge(new LineItem(sourceLedger, ledgerCreditEntry, quantityToCredit, billingMessage),
                            lineItems);
                    });
                });

            orderItemsByDeliveryDocument.forEach((sourceLedger, creditItems) -> {
                credits.add(new BillingCredit(sourceLedger, creditItems));
            });
        }
        return credits;
    }

    public Collection<LineItem> getReturnLines() {
        return returnLines;
    }

    public void setDeliveryDoc(String deliveryDoc) {
        this.deliveryDoc = deliveryDoc;
    }

    public String getDeliveryDoc() {
        return deliveryDoc;
    }



    public void setReturnOrderId(String returnOrderId) {
        this.returnOrderId = returnOrderId;
    }

    public String getReturnOrderId() {
        return returnOrderId;
    }

    public void setReturnOrderInvoiceNotFound() {
        String sapDeliveryDocumentId = getDeliveryDoc();
        if (StringUtils.isNotBlank(sapDeliveryDocumentId)) {
            setReturnOrderId(String.format("%s%s", CREDIT_REQUESTED_PREFIX, sapDeliveryDocumentId));
        }
    }

    public void setBillingResult(BillingEjb.BillingResult billingResult) {
        this.billingResult = billingResult;
    }

    public BillingEjb.BillingResult getBillingResult() {
        return billingResult;
    }

    public static class LineItem {
        private final LedgerEntry ledgerEntry;
        private BigDecimal quantity;
        private BillingMessage billingMessage;
        private LedgerEntry sourceLedger;
        private Set<LineItem> lineItemsToMerge=new HashSet<>();

        LineItem(LedgerEntry sourceLedger, LedgerEntry ledgerEntry, BigDecimal quantity, BillingMessage billingMessage) {
            this.sourceLedger = sourceLedger;
            this.ledgerEntry = ledgerEntry;
            this.quantity = quantity;
            this.billingMessage = billingMessage;
        }

        public static void merge(LineItem lineItem, Set<LineItem> creditItems) {
            Optional<LineItem> foundOrderItem =
                creditItems.stream().filter(l1 -> l1.getSapOrderItem().equals(lineItem.getSapOrderItem())).findFirst();
            if (foundOrderItem.isPresent()) {
                foundOrderItem.get().addLineItemToMerge(lineItem);
            } else {
                creditItems.add(lineItem);
            }
        }

        private void addLineItemToMerge(LineItem lineItem) {
            lineItemsToMerge.add(lineItem);
        }

        public LedgerEntry getLedgerEntry() {
            return ledgerEntry;
        }

        public BigDecimal getQuantity() {
            return lineItemsToMerge.stream().map(LineItem::getQuantity).reduce(quantity, BigDecimal::add);
//            return quantity;
        }

        public void updateQuantity(BigDecimal quantity) {
            this.quantity = this.quantity.add(quantity);
        }

//        public static List<SAPOrderItem> merge(List<SAPOrderItem> orderItems) {
//                  Map<String, List<SAPOrderItem>> orderItemsByPart =
//                      orderItems.stream().collect(Collectors.groupingBy(SAPOrderItem::getProductIdentifier));
//                  List<SAPOrderItem> newList = new ArrayList<>();
//                  orderItemsByPart.forEach((partNumber, sapOrderItems) -> {
//                      BigDecimal quantityOfParts = sapOrderItems.stream().map(SAPOrderItem::getItemQuantity)
//                          .reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
//                      newList.add(new SAPOrderItem(partNumber, quantityOfParts));
//                  });
//                  return newList;
//              }

        public BillingMessage getBillingMessage() {
            return billingMessage;
        }

        public SAPOrderItem getSapOrderItem() {
            String partNumber = getLedgerEntry().getProduct().getPartNumber();
            return new SAPOrderItem(partNumber, quantity);
        }

        public void setSourceLedger(LedgerEntry sourceLedger) {
            this.sourceLedger = sourceLedger;
        }


        public LedgerEntry getSourceLedger() {
            return this.sourceLedger;
        }

        public static void updateCredits(LineItem lineItem) {
            lineItem.getSourceLedger().addCredit(lineItem.ledgerEntry, lineItem.quantity);
            lineItem.lineItemsToMerge.forEach(mergeLineItem->{
                mergeLineItem.getSourceLedger().addCredit(mergeLineItem.ledgerEntry, mergeLineItem.quantity);
            });
        }
    }
}
