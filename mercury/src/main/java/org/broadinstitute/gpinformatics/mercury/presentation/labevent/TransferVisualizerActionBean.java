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
import java.util.Collections;
import java.util.Map;

/**
 * Delivers JSON to the Transfer Visualizer JSP.
 */
@UrlBinding(TransferVisualizerActionBean.ACTION_BEAN_URL)
public class TransferVisualizerActionBean extends CoreActionBean {
    public static final String ACTION_BEAN_URL = "/labevent/transfervis.action";
    public static final String TRANSFER_VIS_PAGE = "/labevent/transfer_vis.jsp";

    private String barcodes;

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
        String[] splitBarcodes = barcodes.split("\\s");
        final Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(Arrays.asList(splitBarcodes));
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
                        Collections.singletonList(TransferVisualizerV2.AlternativeIds.SAMPLE_ID));

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
            return "/labevent/transfervis.action?getJson=&barcodes="+ URLEncoder.encode(barcodes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBarcodes(String barcodes) {
        this.barcodes = barcodes;
    }
}
