package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillableRef;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerImporter;
import org.broadinstitute.gpinformatics.athena.boundary.billing.UploadPreviewData;
import org.broadinstitute.gpinformatics.athena.boundary.orders.OrderBillSummaryStat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
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

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("YYYY-MM-dd-HH:mm:ss-");

    private String filename;

    @ConversationScoped public static class UploadPreviewTableData extends TableData<UploadPreviewData> {}
    @Inject UploadPreviewTableData uploadPreviewTableData;

    @Inject
    private FacesContext facesContext;

    @Inject
    private Conversation conversation;

    public void initView() {
        if (!facesContext.isPostback()) {
            if (conversation.isTransient()) {
                conversation.begin();
            }
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


    public void handleFileUpload(FileUploadEvent event) {

        UploadedFile file = event.getFile();
        //TODO following line just for prototyping
        setFilename(file.getFileName());

        InputStream inputStream = null;

        try {
            BillingTrackerImporter importer = new BillingTrackerImporter(productOrderDao, productOrderSampleDao);

            //TODO This just for initial prototyping.
            inputStream = file.getInputstream();
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
            //TODO correct this
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException( e );
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        // addInfoMessage("Previewing  : " + event.getFile().getFileName() );
        //TODO following line just for prototyping
        setFilename( "BillingTrackerUpload-" + DATE_FORMAT.format(new Date()) + getUsername());
    }

    public String uploadTrackingData() {

        addInfoMessage("Simulated Upload for contents of  : " + getFilename() );

        return null;
    }


    public String cancelUpload() {

        //return to the orders pages
       return redirect("/orders/list");

    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public UploadPreviewTableData getUploadPreviewTableData() {
        return uploadPreviewTableData;
    }
}
