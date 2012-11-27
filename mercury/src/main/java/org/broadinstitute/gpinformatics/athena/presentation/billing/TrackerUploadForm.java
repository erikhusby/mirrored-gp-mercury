package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingTrackerImporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.FileInputStream;

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
    private BillingLedgerDao billingLedgerDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private ProductOrderSampleDao productOrderSampleDao;
    @Inject
    private FacesContext facesContext;

    private String filename;

    public void handleFileUpload(FileUploadEvent event) {
        String productPartNumber=null;

        UploadedFile file = event.getFile();
        //TODO following line just for prototyping
        setFilename(file.getFileName());

        FileInputStream fis = null;

        try {
            BillingTrackerImporter importer = new BillingTrackerImporter(productOrderDao, productOrderSampleDao);

            //TODO This just for initial prototyping.
            fis = (FileInputStream) file.getInputstream();
            productPartNumber = importer.readFromStream ( fis );

        } catch (Exception e) {
            //TODO correct this
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException( e );
        } finally {
            IOUtils.closeQuietly(fis);
        }

        // addInfoMessage("Previewing  : " + event.getFile().getFileName() );
        //TODO following line just for prototyping
        setFilename( productPartNumber );
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

}
