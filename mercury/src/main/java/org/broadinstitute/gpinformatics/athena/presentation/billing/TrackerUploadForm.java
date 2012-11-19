package org.broadinstitute.gpinformatics.athena.presentation.billing;

import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

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
    private FacesContext facesContext;

    private String filename;

    public void handleFileUpload(FileUploadEvent event) {

        UploadedFile file = event.getFile();

        setFilename(file.getFileName());
        // addInfoMessage("Previewing  : " + event.getFile().getFileName() );

    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

}
