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

package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.quote.FundingStatus;
import org.broadinstitute.sap.entity.quote.QuoteStatus;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class QuoteDetailsHelper {
    public static final String QUOTE_INFO = "quoteInfo";
    public static final String ERRORS = "error";
    public static final String WARNINGS = "warning";
    private static final Log logger = LogFactory.getLog(QuoteDetailsHelper.class);

    private QuoteService quoteService;
    private TemplateEngine templateEngine;
    private SapIntegrationService sapService;

    public QuoteDetailsHelper() {
    }

    @Inject
    public QuoteDetailsHelper(QuoteService quoteService, SapIntegrationService sapService, TemplateEngine templateEngine) {
        this.quoteService = quoteService;
        this.templateEngine = templateEngine;
        this.sapService = sapService;
    }

    protected JSONObject getQuoteDetailsJson(ProductOrderActionBean actionBean, String quoteIdentifier)
        throws Exception {
        Map<String, Object> rootMap = new HashMap<>();
        QuoteDetail quoteDetails = getQuoteDetails(quoteIdentifier, actionBean);
        rootMap.put("quoteDetail", quoteDetails);
        StringWriter stringWriter = new StringWriter();
        templateEngine.processTemplate("QuoteDetails.ftl", rootMap, stringWriter);
        JSONObject item = new JSONObject();
        item.put(QUOTE_INFO, stringWriter.toString());
        item.put(ERRORS, quoteDetails.getError());
        item.put(WARNINGS, quoteDetails.hasWarning());
        return item;
    }

    protected QuoteDetail getQuoteDetails(String quoteIdentifier,
                                          ProductOrderActionBean actionBean) throws Exception {
        QuoteDetail quoteDetail = new QuoteDetail();

        try {
            quoteDetail.setQuoteIdentifier(quoteIdentifier);
            if (StringUtils.isNotBlank(quoteIdentifier)) {
                ProductOrder.QuoteSourceType quoteSource = StringUtils.isNumeric(quoteIdentifier) ?
                    ProductOrder.QuoteSourceType.SAP_SOURCE : ProductOrder.QuoteSourceType.QUOTE_SERVER;

                if (!quoteSource.isSapType()) {

                    Quote quote = quoteService.getQuoteByAlphaId(quoteIdentifier);
                    final QuoteFunding quoteFunding = quote.getQuoteFunding();
                    double fundsRemaining = Double.parseDouble(quoteFunding.getFundsRemaining());
                    quoteDetail.setQuoteType(quoteSource);
                    quoteDetail.setFundsRemaining(fundsRemaining);
                    quoteDetail.setStatus(quote.getApprovalStatus().getValue());

                    if(quote.getApprovalStatus() != ApprovalStatus.FUNDED) {
                        quoteDetail.setError("This quote is not yet Funded");
                    }

                    double outstandingOrdersValue = actionBean.estimateOutstandingOrders(quote, 0, null);
                    quoteDetail.setOutstandingEstimate(outstandingOrdersValue);

                    if (CollectionUtils.isEmpty(quoteFunding.getFundingLevel())) {
                        quoteDetail.setError("This quote has no active Funding Sources.");
                    } else {
                        quoteFunding.getFundingLevel().forEach(fundingLevel -> {
                            if (fundingLevel.getFunding() != null) {
                                fundingLevel.getFunding().forEach(funding -> {
                                    if (funding != null && funding.isFundsReservation()) {
                                        if (StringUtils.isNotBlank(funding.getCostObject())) {
                                            FundingInfo fundingInfo = FundingInfo.fundsReservationFunding(
                                                funding.getFundsReservationNumber(), funding.getCostObject(),
                                                funding.getGrantStatus(), funding.getGrantEndDate(),
                                                StringUtils.equals(funding.getGrantStatus(), "Active"));
                                            quoteDetail.addFundingInfo(fundingInfo);
                                        }
                                    } else {
                                        quoteDetail.addFundingInfo(FundingInfo
                                            .purchaseOrderFunding(funding.getPurchaseOrderNumber(),
                                                FundingStatus.APPROVED.getStatusText()));
                                    }
                                });
                            }
                        });
                    }
                } else if (quoteSource.isSapType()) {
                    SapQuote quote = sapService.findSapQuote(quoteIdentifier);
                    Optional<FundingStatus> fundingHeaderStatus =
                        Optional.ofNullable(quote.getQuoteHeader().getFundingHeaderStatus());

                    Optional<OrderCalculatedValues> sapOrderCalculatedValues =
                            Optional.ofNullable(sapService.calculateOpenOrderValues(0, quote, null));

                    quoteDetail.setQuoteType(quoteSource);
                    fundingHeaderStatus.ifPresent(status -> {
                        quoteDetail.setStatus(quote.getQuoteHeader().getQuoteStatus().getStatusText());
                        quoteDetail.setOverallFundingStatus(status.getStatusText());
                    });

                    BigDecimal fundsRemaining = quote.getQuoteHeader().fundsRemaining();
                    if(sapOrderCalculatedValues.isPresent()) {
                        fundsRemaining =fundsRemaining.subtract(sapOrderCalculatedValues.get().openDeliveryValues());
                    }
                    quoteDetail.setFundsRemaining(fundsRemaining.doubleValue());
                    double openOrderEstimate = 0;
                    if(sapOrderCalculatedValues.isPresent()) {
                        openOrderEstimate =
                                sapOrderCalculatedValues.get().calculateTotalOpenOrderValue().doubleValue();
                    }
                    quoteDetail.setOutstandingEstimate(openOrderEstimate);

                    if(quote.getQuoteHeader().getQuoteStatus() != QuoteStatus.Z4) {
                        quoteDetail.setError("This quote has not yet been Approved");
                    }

                    if (CollectionUtils.isEmpty(quote.getFundingDetails())) {
                        quoteDetail.setError("This quote has no active Funding Sources.");
                    } else {
                        quote.getFundingDetails().forEach(fundingDetail -> {
                            try {
                                Optional<SapIntegrationClientImpl.FundingType> fundingType =
                                    Optional.ofNullable(fundingDetail.getFundingType());
                                Optional<FundingStatus> fundingStatus =
                                    Optional.ofNullable(fundingDetail.getFundingStatus());
                                if (fundingType.isPresent()) {
                                    if (fundingType.get() == SapIntegrationClientImpl.FundingType.FUNDS_RESERVATION) {
                                        FundingInfo fundingInfo =
                                            FundingInfo.fundsReservationFunding(fundingDetail.getDocumentNumber(),
                                                fundingDetail.getCostObject(),
                                                fundingDetail.getFundingStatus().getStatusText(),
                                                fundingDetail.getFundingHeaderChangeDate(),
                                                fundingDetail.getFundingStatus() == FundingStatus.APPROVED);
                                        fundingInfo.addSplitPercentage(fundingDetail.getSplitPercentage());
                                        quoteDetail.addFundingInfo(fundingInfo);
                                        if(fundingDetail.getFundingStatus() != FundingStatus.APPROVED) {
                                            quoteDetail.setError("Funding for this quote is not yet fully approved");
                                        }
                                    } else {
                                        quoteDetail.addFundingInfo(FundingInfo
                                            .purchaseOrderFunding(fundingDetail.getCustomerPoNumber(),
                                                fundingDetail.getFundingStatus().getStatusText()));
                                    }
                                } else {
                                    quoteDetail.addFundingInfo(FundingInfo.unknown());
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error occured calculating quote funding", ex);
            try {
                quoteDetail.setError("Unable to complete evaluating order values:  " + ex.getMessage());
            } catch (Exception ex1) {
                // Don't really care if this gets an exception.
            }
        }
        return quoteDetail;
    }

    public static class FundingInfo {
        public static final String PDO_DETAIL_FORMAT = "Purchase Order [<b>%s</b>] : %s";
        public static final String FUNDS_RESERVATION_DETAIL_FORMAT =
            "Funds Reservation [<b>FR -- %s, CO -- %s</b>] : %s -- %s";
        private String fundingType;
        private boolean quoteWarning = false;
        private String fundingInfoString;

        public FundingInfo(String fundingType) {
            this.fundingType = fundingType;
        }

        public FundingInfo() {
            this(null);
        }

        public static FundingInfo purchaseOrderFunding(String pdoName, String status) {
            FundingInfo fundingInfo = new FundingInfo(Funding.PURCHASE_ORDER);
            if (!status.equals(FundingStatus.APPROVED.getStatusText()) || !status
                .equals(ApprovalStatus.APPROVED.getValue())) {
                fundingInfo.setQuoteWarning(true);
            }
            fundingInfo.setFundingInfoString(String.format(PDO_DETAIL_FORMAT, pdoName, status));
            return fundingInfo;
        }

        public static FundingInfo unknown() {
            FundingInfo fundingInfo = new FundingInfo();
            fundingInfo.setFundingInfoString("unable to determine funding type");
            fundingInfo.setQuoteWarning(true);
            return fundingInfo;
        }

        public static FundingInfo fundsReservationFunding(String fundsReservationNumber, String costObject,
                                                          String status, Date expirationDate, boolean isApproved) {
            FundingInfo fundingInfo = new FundingInfo(Funding.FUNDS_RESERVATION);
            fundingInfo.setQuoteWarning(!isApproved);

            Date todayTruncated = org.apache.commons.lang3.time.DateUtils.truncate(new Date(), Calendar.DATE);

            boolean grantActive = FundingLevel.isGrantActiveForDate(todayTruncated, expirationDate);
            long daysUntilExpired = DateUtils.getNumDaysBetween(todayTruncated, expirationDate);


            String expiresString = "Expires " + DateUtils.getDate(expirationDate);
            if (daysUntilExpired > 0 && daysUntilExpired < 45) {
                expiresString = String.format(
                    "Expires in %d days. If it is likely this work will not be completed by then, please work on updating the "
                    + "Funding Source so billing errors can be avoided.", daysUntilExpired);
                fundingInfo.setQuoteWarning(true);
            } else if (daysUntilExpired <= 0) {
                expiresString = "Expired";
                fundingInfo.setQuoteWarning(true);
            }
            String fundingInfoString = String.format(FUNDS_RESERVATION_DETAIL_FORMAT, fundsReservationNumber,
                costObject, status, expiresString);
            fundingInfo.setFundingInfoString(fundingInfoString);
            return fundingInfo;
        }

        public String getFundingInfoString() {
            return fundingInfoString;
        }

        public void setFundingInfoString(String fundingInfoString) {
            this.fundingInfoString = fundingInfoString;
        }

        public boolean isQuoteWarning() {
            return quoteWarning;
        }

        public void setQuoteWarning(boolean quoteWarning) {
            this.quoteWarning = quoteWarning;
        }

        public void addSplitPercentage(String split) {
            String splitPercent = (split.endsWith("%")) ? split : split + "%";
            fundingInfoString =
                String.format("%s<br>funding split percentage = %s", fundingInfoString, splitPercent);
        }

        public void addSplitPercentage(BigDecimal splitPercentage) {
            Optional.ofNullable(splitPercentage)
                .filter(splitPct -> splitPct.compareTo(BigDecimal.ZERO) > 0)
                .ifPresent(splitPct -> addSplitPercentage(splitPct.toPlainString() + "%"));
        }
    }

    public class QuoteDetail {
        public static final String FUNDS_REMAINING_FORMAT =
            "Status: %s - Funds Remaining: %s with %s unbilled across existing open orders";
        List<FundingInfo> fundingDetails = new ArrayList<>();
        Double fundsRemaining;
        Double outstandingEstimate;
        ProductOrder.QuoteSourceType quoteType;
        String quoteIdentifier;
        String status;
        String error;
        private String overallFundingStatus;

        public String getFundsRemaining() {
            String fundsRemainingString = "";
            String formattedFundsRemaining =
                NumberFormat.getCurrencyInstance().format(Optional.ofNullable(fundsRemaining).orElse(0D));
            String outstandingEstimateString =
                NumberFormat.getCurrencyInstance().format(Optional.ofNullable(outstandingEstimate).orElse(0D));
            if (!quoteType.isSapType()) {
                fundsRemainingString =
                    String.format(FUNDS_REMAINING_FORMAT, status, formattedFundsRemaining, outstandingEstimateString);
            } else {
                fundsRemainingString = String.format(FUNDS_REMAINING_FORMAT,
                    String.format("%s, Funding Status: %s ", status, overallFundingStatus), formattedFundsRemaining,
                    outstandingEstimateString);
            }
            return fundsRemainingString;
        }

        public void setFundsRemaining(Double fundsRemaining) {
            this.fundsRemaining = fundsRemaining;
        }

        public List<FundingInfo> getFundingDetails() {
            return fundingDetails;
        }

        public ProductOrder.QuoteSourceType getQuoteType() {
            return quoteType;
        }

        public void setQuoteType(ProductOrder.QuoteSourceType quoteType) {
            this.quoteType = quoteType;
        }

        public String getQuoteIdentifier() {
            return quoteIdentifier;
        }

        public void setQuoteIdentifier(String quoteIdentifier) {
            this.quoteIdentifier = quoteIdentifier;
        }

        public Double getOutstandingEstimate() {
            return outstandingEstimate;
        }

        public void setOutstandingEstimate(Double outstandingEstimate) {
            this.outstandingEstimate = outstandingEstimate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOverallFundingStatus() {
            return overallFundingStatus;
        }

        public void setOverallFundingStatus(String overallFundingStatus) {
            this.overallFundingStatus = overallFundingStatus;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public boolean hasWarning() {
            return getFundingDetails().stream().anyMatch(FundingInfo::isQuoteWarning);
        }

        public void addFundingInfo(FundingInfo fundingInfo) {
            fundingDetails.add(fundingInfo);
            if (hasWarning() && fundingInfo.fundingType.equals(Funding.FUNDS_RESERVATION)) {
                fundingDetails.forEach(fi->fi.setQuoteWarning(true));
            }
        }
    }
}


