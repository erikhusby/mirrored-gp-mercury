package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

@UrlBinding(value = "/view/uploadFingerprintingRun.action")
public class UploadFingerprintingRunActionBean extends CoreActionBean {

    public static final String UPLOAD_ACTION = "upload";

    public static final String FINGERPRINTING_RUN_UPLOAD_PAGE = "/vessel/fingerprinting_run_upload.jsp";

    @Validate(required = true, on = UPLOAD_ACTION)
    private FileBean runFile;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(FINGERPRINTING_RUN_UPLOAD_PAGE);
    }

    @HandlesEvent(UPLOAD_ACTION)
    public Resolution upload() {
        MessageCollection messageCollection = new MessageCollection();

        addMessages(messageCollection);
        return new ForwardResolution(FINGERPRINTING_RUN_UPLOAD_PAGE);
    }

    public void setRunFile(FileBean runFile) {
        this.runFile = runFile;
    }
}
