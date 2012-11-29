package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillableRef;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerImporter;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerManager;
import org.broadinstitute.gpinformatics.athena.boundary.billing.UploadPreviewData;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jsf.TableData;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 11/19/12
 * Time: 1:00 PM
 */
@Named("fileUploadController")
@RequestScoped
public class TrackerUploadForm  extends AbstractJsfBean {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @ConversationScoped
    public static class UploadPreviewTableData extends TableData<UploadPreviewData> {}
    @Inject UploadPreviewTableData uploadPreviewTableData;

    @ConversationScoped
    @Inject
    private BillingUploadConversationData conversationData;

    @Inject
    private Conversation conversation;

    @Inject
    private FacesContext facesContext;

    @Inject
    private UserTransaction utx;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss-");


    public void initView() {
        if (!facesContext.isPostback()) {
            conversation.begin();
        }
    }

    /**
     * Is there a common location for this logic?
     *
     * @return
     */
    private String getUsername() {
        return ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).
                getUserPrincipal().getName();
    }


    public void handleFileUploadForPreview(FileUploadEvent event) {
        String fileType = null;
        UploadedFile file = event.getFile();

        // Check the fileType
        if (( file != null ) && "application/vnd.ms-excel".equalsIgnoreCase( file.getContentType())) {
            InputStream inputStream = null;

            previewUploadedFile(file, inputStream);

        } else {
            addErrorMessage("Cannot upload this type of file (" + fileType + " ). Must be Excel." );
        }
    }

    private void previewUploadedFile(UploadedFile file, InputStream inputStream) {
        try {
            inputStream = file.getInputstream();

            BillingTrackerImporter importer = new BillingTrackerImporter(productOrderDao, productOrderSampleDao);

            Map<String, Map<String,Map<BillableRef,OrderBillSummaryStat>>> productProductOrderPriceItemChargesMap = importer.parseFileForSummaryMap(inputStream);

            List<UploadPreviewData> uploadPreviewData = new ArrayList<UploadPreviewData>();

            // for the purposes of preview we don't actually care about the product keys in this nested map, only
            // the product orders and price items
            Collection<Map<String,Map<BillableRef,OrderBillSummaryStat>>> productOrderToBillableRefsMap =
                    productProductOrderPriceItemChargesMap.values();

            // keys are product order bizkeys, values are maps of billable refs (product + price item) to bill stats
            for (Map<String, Map<BillableRef, OrderBillSummaryStat>> entry : productOrderToBillableRefsMap) {
                for (Map.Entry<String, Map<BillableRef, OrderBillSummaryStat>> pdoEntry : entry.entrySet()) {
                    String pdoKey = pdoEntry.getKey();

                    for (Map.Entry<BillableRef, OrderBillSummaryStat> value : pdoEntry.getValue().entrySet()) {
                        String partNumber = value.getKey().getProductPartNumber();
                        String priceItem = value.getKey().getPriceItemName();

                        Double charges = value.getValue().getCharge();
                        Double credits = value.getValue().getCredit();

                        uploadPreviewData.add(new UploadPreviewData(pdoKey, partNumber, priceItem, charges, credits));
                    }
                }
            }

            Collections.sort(uploadPreviewData);
            uploadPreviewTableData.setValues(uploadPreviewData);

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException( e );
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        InputStream fis=null;
        try {
            BillingTrackerImporter importer = new BillingTrackerImporter(productOrderDao, productOrderSampleDao);
            fis = file.getInputstream();
            File tempFile = importer.copyFromStreamToTempFile(fis);
            //Keep the filename in conversation scope
            conversationData.setFilename( tempFile.getAbsolutePath() );
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new RuntimeException( e );
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

//    /*
//     * The following method is in temporarily until we can get conversation scope working.  !!!!!
//     */
//    public void uploadBillingDirectlyTEMP(FileUploadEvent event) {
//
//        UploadedFile file = event.getFile();
//
//        // Check the fileType
//        if (( file != null ) && "application/vnd.ms-excel".equalsIgnoreCase( file.getContentType())) {
//            InputStream fis=null;
//            File tempFile = null;
//            try {
//                BillingTrackerImporter importer = new BillingTrackerImporter(productOrderDao, productOrderSampleDao);
//                fis = file.getInputstream();
//                tempFile = importer.copyFromStreamToTempFile(fis);
//            } catch ( Exception e ) {
//                e.printStackTrace();
//                throw new RuntimeException( e );
//            } finally {
//                IOUtils.closeQuietly(fis);
//            }
//
//            processBillingOnTempFile(tempFile);
//
//        } else {
//          addInfoMessage("Could not Upload. Filename is blank." );
//        }
//    }

    public String uploadTrackingDataForBilling() {

        String tempFilename = conversationData.getFilename();
        File tempFile = null;

        if ( StringUtils.isNotBlank( tempFilename ) ) {
            tempFile = new File( tempFilename );

            processBillingOnTempFile(tempFile);

        } else {
          addInfoMessage("Could not Upload. Filename is blank." );
        }

        return null;

    }

    private void processBillingOnTempFile(File tempFile) {
        InputStream inputStream = null;
        try {
            inputStream =  new FileInputStream(tempFile);

            utx.begin();

            BillingTrackerManager manager = new BillingTrackerManager(productOrderDao, billingLedgerDao);

            Map<String, List<ProductOrder>> billedProductOrdersMapByPartNumber = manager.parseFileForBilling(inputStream);

            int numberOfProducts = 0;
            List<String> orderIdsUpdated = new ArrayList<String>();
            if ( billedProductOrdersMapByPartNumber != null ) {
                numberOfProducts = billedProductOrdersMapByPartNumber.keySet().size();
                orderIdsUpdated = extractOrderIdsFromMap(billedProductOrdersMapByPartNumber);
            }

            utx.commit();

            // Set filename to null to disable the upload button and prevent re-billing.
            conversationData.setFilename( null );

            addInfoMessage("Updated the billing ledger for  : " + orderIdsUpdated.size() + " Product Order(s) for " +
                    numberOfProducts + " primary product(s)." );

        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (SystemException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            addErrorMessage(e.getMessage());
            throw new RuntimeException( e );
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }


    private List<String> extractOrderIdsFromMap(Map<String, List<ProductOrder>> billedProductOrdersMapByPartNumber ) {
        List<String> orderIdsUpdated = new ArrayList<String>();

        if ( billedProductOrdersMapByPartNumber != null ) {
            for (String productPartNumberStr : billedProductOrdersMapByPartNumber.keySet() ) {
                List<ProductOrder> sheetOrders = billedProductOrdersMapByPartNumber.get( productPartNumberStr );
                if ( sheetOrders != null ) {
                    for (ProductOrder productOrder : sheetOrders ) {
                        if ( productOrder != null ) {
                            String productOrderIdStr = productOrder.getBusinessKey();
                            if (StringUtils.isNotBlank(productOrderIdStr)) {
                                orderIdsUpdated.add( productOrder.getBusinessKey() );
                            }
                        }
                    }
                }
            }
        }
        return orderIdsUpdated;
    }


    public String cancelUpload() {

        conversationData.setFilename( null );

        //return to the orders pages
       return redirect("/orders/list");

    }

    public boolean getHasFilename() {
        return ( StringUtils.isNotBlank( conversationData.getFilename() ));
    }

    public UploadPreviewTableData getUploadPreviewTableData() {
        return uploadPreviewTableData;
    }
}
