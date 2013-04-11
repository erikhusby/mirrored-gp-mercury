package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillableRef;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerImporter;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerManager;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.*;
import java.util.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/orders/uploadTracker.action")
public class UploadTrackerActionBean extends CoreActionBean {

    private static final String TRACKER_PAGE = "/orders/uploadTracker.jsp";

    @Inject
    private Log logger;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private PriceListCache priceListCache;

    @Validate(required = true, on = "preview")
    private FileBean trackerFile;

    @Validate(required = true, on = "upload")
    private String previewFilePath;

    private List<PreviewData> previewData;

    private boolean isPreview = false;

    @Inject
    private BillingTrackerManager billingTrackerManager;

    public List<PreviewData> getPreviewData() {
        return previewData;
    }

    private void previewUploadedFile() {
        InputStream inputStream = null;

        try {
            inputStream = trackerFile.getInputStream();

            BillingTrackerImporter importer =
                new BillingTrackerImporter(productOrderDao, priceItemDao, priceListCache, getContext().getValidationErrors());
            Map<String, Map<String, Map<BillableRef, OrderBillSummaryStat>>> productProductOrderPriceItemChargesMap =
                    importer.parseFileForSummaryMap(inputStream);

            // If there were parsing errors, just return
            if (hasErrors()) {
                return;
            }

            previewData = new ArrayList<PreviewData>();

            // For the purposes of preview we don't actually care about the product keys in this nested map, only
            // the product orders and price items.
            Collection<Map<String, Map<BillableRef, OrderBillSummaryStat>>> productOrderToBillableRefsMap =
                    productProductOrderPriceItemChargesMap.values();

            Set<String> automatedPDOs = new HashSet<String>();

            // Keys are product order business keys, values are maps of billable refs (product + price item) to
            // bill stats.
            for (Map<String, Map<BillableRef, OrderBillSummaryStat>> entry : productOrderToBillableRefsMap) {
                for (Map.Entry<String, Map<BillableRef, OrderBillSummaryStat>> pdoEntry : entry.entrySet()) {
                    String pdoKey = pdoEntry.getKey();

                    ProductOrder order = productOrderDao.findByBusinessKey(pdoKey);
                    if (order != null && order.getProduct().isUseAutomatedBilling()) {
                        automatedPDOs.add(pdoKey);
                    }

                    for (Map.Entry<BillableRef, OrderBillSummaryStat> value : pdoEntry.getValue().entrySet()) {
                        String partNumber = value.getKey().getProductPartNumber();
                        String priceItem = value.getKey().getPriceItemName();

                        double charges = value.getValue().getCharge();
                        double credits = value.getValue().getCredit();

                        previewData.add(new PreviewData(pdoKey, partNumber, priceItem, charges, credits));
                    }
                }
            }

            if (previewData.isEmpty()) {
                addMessage("All updated fields match billed amounts. Any previously uploaded, unbilled items will be removed");
            }

            if (!automatedPDOs.isEmpty()) {
                addGlobalValidationError("Cannot upload data for these product orders because they use " +
                        "automated billing: " + StringUtils.join(automatedPDOs, ", "));
                return;
            }

            // Close the file that was just read in so we can get ready to copy to our temp directory
            IOUtils.closeQuietly(inputStream);

            // Even if there is no preview data, we may want to clear out previously billed items, so do all this work either way.
            inputStream = trackerFile.getInputStream();
            File tempFile = copyFromStreamToTempFile(inputStream);
            previewFilePath = tempFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error(e);
            addGlobalValidationError("Error uploading tracker: " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static File copyFromStreamToTempFile(InputStream is) throws IOException {
        Date now = new Date();
        File tempFile = File.createTempFile("BillingTrackerTempFile_" + now.getTime(), ".xls");

        OutputStream out = new FileOutputStream(tempFile);

        try {
            IOUtils.copy(is, out);
        } finally {
            IOUtils.closeQuietly(out);
        }

        return tempFile.getAbsoluteFile();
    }

    private void processBillingOnTempFile() {
        InputStream inputStream = null;

        try {
            File previewFile = new File(previewFilePath);
            inputStream = new FileInputStream(previewFile);

            Map<String, List<ProductOrder>> billedProductOrdersMapByPartNumber =
                    billingTrackerManager.parseFileForBilling(inputStream);

            if (hasErrors()) {
                return;
            }

            int numberOfProducts = 0;
            List<String> orderIdsUpdated = new ArrayList<String>();
            if (billedProductOrdersMapByPartNumber != null) {
                numberOfProducts = billedProductOrdersMapByPartNumber.keySet().size();
                orderIdsUpdated = extractOrderIdsFromMap(billedProductOrdersMapByPartNumber);
            }

            addMessage("Updated the billing ledger for " + orderIdsUpdated.size() + " product order(s) across " +
                            numberOfProducts + " primary product(s).");

            // Since everything worked, delete the file.
            FileUtils.deleteQuietly(previewFile);
        } catch (Exception e) {
            logger.error(e);
            addGlobalValidationError(e.getMessage());
        } finally {
            // No matter what, close the file but it will be ignored if everything was closed and deleted.
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static List<String> extractOrderIdsFromMap(
            Map<String, List<ProductOrder>> billedProductOrdersMapByPartNumber) {
        List<String> orderIdsUpdated = new ArrayList<String>();

        if (billedProductOrdersMapByPartNumber != null) {
            for (String productPartNumberStr : billedProductOrdersMapByPartNumber.keySet()) {
                List<ProductOrder> sheetOrders = billedProductOrdersMapByPartNumber.get(productPartNumberStr);
                if (sheetOrders != null) {
                    for (ProductOrder productOrder : sheetOrders) {
                        if (productOrder != null) {
                            String productOrderIdStr = productOrder.getBusinessKey();
                            if (StringUtils.isNotBlank(productOrderIdStr)) {
                                orderIdsUpdated.add(productOrderIdStr);
                            }
                        }
                    }
                }
            }
        }

        return orderIdsUpdated;
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        isPreview = false;
        return new ForwardResolution(TRACKER_PAGE);
    }

    @HandlesEvent("upload")
    public Resolution upload() {
        isPreview = false;
        processBillingOnTempFile();
        return new ForwardResolution(TRACKER_PAGE);
    }

    @HandlesEvent("preview")
    public Resolution preview() {
        previewUploadedFile();

        // If there are no errors, show the preview!
        if (!hasErrors()) {
            isPreview = true;
        }

        return new ForwardResolution(TRACKER_PAGE);
    }

    public boolean getIsPreview() {
        return isPreview;
    }

    public String getPreviewFilePath() {
        return previewFilePath;
    }

    public void setPreviewFilePath(String previewFilePath) {
        this.previewFilePath = previewFilePath;
    }

    public void setTrackerFile(FileBean trackerFile) {
        this.trackerFile = trackerFile;
    }

    public static class PreviewData {
        private final String order;
        private final String partNumber;
        private final String priceItemName;
        private final double newCharges;
        private final double newCredits;

        public PreviewData(String order, String partNumber, String priceItemName, double newCharges, double newCredits) {
            this.order = order;
            this.partNumber = partNumber;
            this.priceItemName = priceItemName;
            this.newCharges = newCharges;
            this.newCredits = newCredits;
        }

        public String getOrder() {
            return order;
        }

        public String getPartNumber() {
            return partNumber;
        }

        public String getPriceItemName() {
            return priceItemName;
        }

        public double getNewCharges() {
            return newCharges;
        }

        public double getNewCredits() {
            return newCredits;
        }
    }

}
