package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.search.SearchRequestBean;
import org.broadinstitute.gpinformatics.mercury.boundary.search.SearchValueBean;
import org.broadinstitute.gpinformatics.mercury.control.vessel.FluidigmRunFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.ConfigurableSearchActionBean;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@UrlBinding(value = "/view/uploadFingerprintingRun.action")
public class UploadFingerprintingRunActionBean extends CoreActionBean {

    public static final String UPLOAD_ACTION = "upload";

    public static final String FINGERPRINTING_RUN_UPLOAD_PAGE = "/vessel/fingerprinting_run_upload.jsp";

    @Inject
    private FluidigmRunFactory fluidigmRunFactory;

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
        InputStream fileStream = null;
        try {
            fileStream = runFile.getInputStream();
            Pair<StaticPlate, LabMetricRun> pair = fluidigmRunFactory.createFluidigmChipRun(runFile.getInputStream(),
                    userBean.getBspUser().getUserId(), messageCollection);
            LabMetricRun run = pair.getRight();
            if (!messageCollection.hasErrors() && run != null) {
                messageCollection.addInfo("Successfully uploaded run " + run.getRunName());
                String url = buildUrl(pair.getLeft().getLabel());
                return new RedirectResolution(url);
            }
            addMessages(messageCollection);
        } catch (IOException io) {
            messageCollection.addError("IO exception while parsing upload."  + io.getMessage());
        } finally {
            IOUtils.closeQuietly(fileStream);
            try {
                runFile.delete();
            } catch (IOException ignored) {
                // If cannot delete, oh well.
            }
        }
        return new ForwardResolution(FINGERPRINTING_RUN_UPLOAD_PAGE);
    }

    public String buildUrl(String chipBarcode) throws IOException {

        StringBuilder link = new StringBuilder()
                .append("/search/ConfigurableSearch.action?")
                .append(ConfigurableSearchActionBean.DRILL_DOWN_EVENT)
                .append("=&drillDownRequest=");

        SearchRequestBean searchRequest = new SearchRequestBean("LabVessel", "GLOBAL|GLOBAL_LAB_VESSEL_SEARCH_INSTANCES|Fluidigm Chip Drill Down",
                Collections.singletonList(new SearchValueBean("Fluidigm Chip Barcode", Collections.singletonList(chipBarcode))));
        link.append(new ObjectMapper().writeValueAsString(searchRequest));
        return link.toString();
    }

    public void setRunFile(FileBean runFile) {
        this.runFile = runFile;
    }

}
