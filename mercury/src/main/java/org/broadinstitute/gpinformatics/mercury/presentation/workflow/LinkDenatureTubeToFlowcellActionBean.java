package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;

@UrlBinding("/workflow/LinkDenatureTubeToFlowcell.action")
public class LinkDenatureTubeToFlowcellActionBean extends CoreActionBean {
    private static String VIEW_PAGE = "/resources/workflow/link_dtube_to_fc.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private AthenaClientService athenaClientService;

    @Validate(required = true, on = SAVE_ACTION)
    private String denatureTubeBarcode;

    @Validate(required = true, on = SAVE_ACTION)
    private String flowcellBarcode;

    private LabVessel denatureTube;

    private String workflowName;

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public void setFlowcellBarcode(String flowcellBarcode) {
        this.flowcellBarcode = flowcellBarcode;
    }

    public String getDenatureTubeBarcode() {
        return denatureTubeBarcode;
    }

    public void setDenatureTubeBarcode(String denatureTubeBarcode) {
        this.denatureTubeBarcode = denatureTubeBarcode;
    }

    public LabVessel getDenatureTube() {
        return denatureTube;
    }

    public void setDenatureTube(LabVessel denatureTube) {
        this.denatureTube = denatureTube;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    @DefaultHandler
    public ForwardResolution showPage() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public ForwardResolution save() {
        //send event
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateData() {
        denatureTube = labVesselDao.findByIdentifier(denatureTubeBarcode);
        if (denatureTube == null) {
            addValidationError("denatureTubeBarcode", "Could not find denature tube {0}", denatureTubeBarcode);
        }
    }

    @HandlesEvent("denatureInfo")
    public ForwardResolution denatureTubeInfo() {
        denatureTube = labVesselDao.findByIdentifier(denatureTubeBarcode);
        if (denatureTube != null) {
            for (SampleInstance sample : denatureTube.getAllSamples()) {
                String productOrderKey = sample.getStartingSample().getProductOrderKey();
                if (StringUtils.isNotEmpty(productOrderKey)) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    workflowName = order.getProduct().getWorkflowName();
                    break;
                }
            }
        }
        return new ForwardResolution("/resources/workflow/denature_tube_info.jsp");
    }
}
