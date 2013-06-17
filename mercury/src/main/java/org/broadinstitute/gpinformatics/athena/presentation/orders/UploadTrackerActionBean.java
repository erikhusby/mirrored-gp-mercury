package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillableRef;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerManager;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerProcessor;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/orders/uploadTracker.action")
public class UploadTrackerActionBean extends CoreActionBean {

    private static final String TRACKER_PAGE = "/orders/uploadTracker.jsp";

    @Inject
    private Log logger;

    @Inject
    private ProductDao productDao;

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

        List<String> sheetNames = PoiSpreadsheetParser.getWorksheetNames(trackerFile);
        Map<String, BillingTrackerProcessor> processors = getProcessors(sheetNames);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(processors);

        try {
            parser.processUploadFile(trackerFile.getInputStream());

            // If there were parsing errors, just return.
            if (hasErrors()) {
                return;
            }

            previewData = new ArrayList<>();

            // Separate out the complex structure into charges and credits and make sure to get list of auto billed entries.
            Set<String> automatedPDOs = new HashSet<>();
            separateChargesAndCredits(sheetNames, processors, automatedPDOs);

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

    /**
     * Take the sheet names and create a new processor for each one.
     *
     * @param sheetNames The names of the sheets (should be all part numbers of products.
     *
     * @return The mapping of sheet names to processors.
     */
    private Map<String, BillingTrackerProcessor> getProcessors(List<String> sheetNames) {
        Map<String, BillingTrackerProcessor> processors = new HashMap<> ();

        for (String sheetName : sheetNames) {
            BillingTrackerProcessor processor = new BillingTrackerProcessor(
                    sheetName, productDao, productOrderDao, priceItemDao, priceListCache, getContext().getValidationErrors());
            processors.put(sheetName, processor);
        }

        return processors;
    }

    private void separateChargesAndCredits(
            List<String> sheetNames, Map<String, BillingTrackerProcessor> processors, Set<String> automatedPDOs) {

        for (String sheetName : sheetNames) {
            BillingTrackerProcessor processor = processors.get(sheetName);

            Map<String, Map<BillableRef, OrderBillSummaryStat>> chargesMapByPdo = processor.getChargesMapByPdo();
            for (Map.Entry<String, Map<BillableRef, OrderBillSummaryStat>> pdoEntry : chargesMapByPdo.entrySet()) {
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
            List<String> orderIdsUpdated = new ArrayList<>();
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
        List<String> orderIdsUpdated = new ArrayList<>();

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

    @SuppressWarnings("unused")
    public void setPreviewFilePath(String previewFilePath) {
        this.previewFilePath = previewFilePath;
    }

    @SuppressWarnings("unused")
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
