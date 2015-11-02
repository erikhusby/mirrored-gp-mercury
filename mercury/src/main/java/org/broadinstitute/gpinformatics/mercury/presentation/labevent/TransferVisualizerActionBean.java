package org.broadinstitute.gpinformatics.mercury.presentation.labevent;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.boundary.transfervis.TransferVisualizerV2;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    private List<String> barcodes = new ArrayList<>();

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private TransferVisualizerV2 transferVisualizerV2;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(TRANSFER_VIS_PAGE);
    }

    public Resolution visualize() {
        return new ForwardResolution(TRANSFER_VIS_PAGE);
    }

    public Resolution getJson() {
        final Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
        return new Resolution() {
            @Override
            public void execute(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                    throws IOException {
                httpServletResponse.setContentType("text");
                transferVisualizerV2.jsonForVessels(
                        new ArrayList<>(mapBarcodeToVessel.values()),
                        Arrays.asList(TransferTraverserCriteria.TraversalDirection.Ancestors,
                                TransferTraverserCriteria.TraversalDirection.Descendants),
                        httpServletResponse.getWriter());

            }
        };
    }

    public List<String> getBarcodes() {
        return barcodes;
    }

    public void setBarcodes(List<String> barcodes) {
        this.barcodes = barcodes;
    }
}
