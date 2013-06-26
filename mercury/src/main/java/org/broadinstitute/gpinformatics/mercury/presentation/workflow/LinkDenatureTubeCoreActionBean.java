package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;

public class LinkDenatureTubeCoreActionBean extends CoreActionBean {
    @Inject
    protected LabVesselDao labVesselDao;
    @Inject
    protected AthenaClientService athenaClientService;
    @Inject
    protected BettalimsMessageResource bettalimsMessageResource;
    @Inject
    protected VesselTransferEjb vesselTransferEjb;

    @Validate(required = true, on = SAVE_ACTION)
    public String denatureTubeBarcode;
    private TwoDBarcodedTube denatureTube;
    protected String workflowName;

    public LabVessel getDenatureTube() {
        if (denatureTube == null && !denatureTubeBarcode.isEmpty()) {
            loadDenatureTubeAndWorkflow();
        }
        return denatureTube;
    }

    public void setDenatureTube(TwoDBarcodedTube denatureTube) {
        this.denatureTube = denatureTube;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public String getDenatureTubeBarcode() {
        return denatureTubeBarcode;
    }

    public void setDenatureTubeBarcode(String denatureTubeBarcode) {
        this.denatureTubeBarcode = denatureTubeBarcode;
    }

    @HandlesEvent("denatureInfo")
    public ForwardResolution denatureTubeInfo() {
        loadDenatureTubeAndWorkflow();
        return new ForwardResolution("/workflow/denature_tube_info.jsp");
    }

    private void loadDenatureTubeAndWorkflow() {
        denatureTube = (TwoDBarcodedTube) labVesselDao.findByIdentifier(denatureTubeBarcode);
        if (denatureTube != null) {
            for (SampleInstance sample : denatureTube.getAllSamplesOfType(LabVessel.SampleType.WITH_PDO)) {
                String productOrderKey = sample.getProductOrderKey();
                if (StringUtils.isNotEmpty(productOrderKey)) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    workflowName = order.getProduct().getWorkflowName();
                    break;
                }
            }
        }
    }
}
