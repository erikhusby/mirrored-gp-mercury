package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizerV2;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Delivers JSON to the Transfer Visualizer JSP.
 */
@UrlBinding(TransferVisualizerActionBean.ACTION_BEAN_URL)
public class TransferVisualizerActionBean extends CoreActionBean {
    public static final String ACTION_BEAN_URL = "/labevent/transfervis.action";
    public static final String TRANSFER_VIS_PAGE = "/labevent/transfer_vis.jsp";

    /** POSTed from form */
    private String barcodes;
    /** POSTed from form */
    private List<TransferVisualizerV2.AlternativeIds> alternativeIds = new ArrayList<>();

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private TransferVisualizerV2 transferVisualizerV2;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(TRANSFER_VIS_PAGE);
    }

    /**
     * Called when the user clicks the Visualize button.
     */
    public Resolution visualize() {
        if (barcodes == null) {
            addValidationError("barcodes", "Enter at least one barcode.");
            return new ForwardResolution(TRANSFER_VIS_PAGE);
        }
        String[] splitBarcodes = barcodes.split("\\s");
        Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(Arrays.asList(splitBarcodes));
        for (Map.Entry<String, LabVessel> barcodeLabVesselEntry : mapBarcodeToVessel.entrySet()) {
            if (barcodeLabVesselEntry.getValue() == null) {
                addValidationError("barcodes", barcodeLabVesselEntry.getKey() + " not found.");
            }
        }
        return new ForwardResolution(TRANSFER_VIS_PAGE);
    }

    /**
     * Called through AJAX after the page has rendered.
     */
    public Resolution getJson() {
        String[] splitBarcodes = barcodes.split("\\s");
        final Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(Arrays.asList(splitBarcodes));
        return new Resolution() {
            @Override
            public void execute(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                    throws IOException {
                httpServletResponse.setContentType("text");
                transferVisualizerV2.jsonForVessels(
                        new ArrayList<>(mapBarcodeToVessel.values()),
                        Arrays.asList(TransferTraverserCriteria.TraversalDirection.Ancestors,
                                TransferTraverserCriteria.TraversalDirection.Descendants),
                        httpServletResponse.getWriter(),
                        alternativeIds);
                labVesselDao.clear();
            }
        };
    }

    public String getBarcodes() {
        return barcodes;
    }

    public String getJsonUrl() {
        try {
            if (hasErrors() || StringUtils.isEmpty(barcodes)) {
                return null;
            }
            String url = "/labevent/transfervis.action?getJson=&barcodes=" + URLEncoder.encode(barcodes, "UTF-8");
            for (TransferVisualizerV2.AlternativeIds alternativeId : alternativeIds) {
                url += "&alternativeIds=" + alternativeId.toString();
            }

            return url;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBarcodes(String barcodes) {
        this.barcodes = barcodes;
    }

    public List<TransferVisualizerV2.AlternativeIds> getAlternativeIds() {
        return alternativeIds;
    }

    public void setAlternativeIds(List<TransferVisualizerV2.AlternativeIds> alternativeIds) {
        this.alternativeIds = alternativeIds;
    }
}
