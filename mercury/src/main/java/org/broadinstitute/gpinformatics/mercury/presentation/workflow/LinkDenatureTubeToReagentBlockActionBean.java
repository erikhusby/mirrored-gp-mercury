package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLimsMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@UrlBinding("/workflow/LinkDenatureTubeToReagentBlock.action")
public class LinkDenatureTubeToReagentBlockActionBean extends CoreActionBean {
    private static String VIEW_PAGE = "/workflow/link_dtube_to_rb.jsp";

    @Validate(required = true, on = SAVE_ACTION)
    private String reagentBlockBarcode;
    @Inject
    protected LabVesselDao labVesselDao;
    @Inject
    protected AthenaClientService athenaClientService;
    @Inject
    protected BettaLimsMessageResource bettaLimsMessageResource;
    @Inject
    protected VesselTransferEjb vesselTransferEjb;
    @Validate(required = true, on = SAVE_ACTION)
    public String denatureTubeBarcode;
    private TwoDBarcodedTube denatureTube;
    protected String workflowName;

    public String getReagentBlockBarcode() {
        return reagentBlockBarcode;
    }

    public void setReagentBlockBarcode(String reagentBlockBarcode) {
        this.reagentBlockBarcode = reagentBlockBarcode;
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public ForwardResolution showPage() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        Map<String, VesselPosition> denatureMap = new HashMap<>();
        denatureMap.put(denatureTubeBarcode, VesselPosition.A01);

        BettaLimsMessage bettaLimsMessage = vesselTransferEjb
                .denatureToReagentKitTransfer(null, denatureMap, reagentBlockBarcode,
                        getUserBean().getLoginUserName(), "UI");
        bettaLimsMessageResource.processMessage(bettaLimsMessage);

        addMessage("Denature Tube {0} associated with Reagent Block {1}", denatureTubeBarcode,
                reagentBlockBarcode);
        return new RedirectResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateData() {
        setDenatureTube((TwoDBarcodedTube) labVesselDao.findByIdentifier(denatureTubeBarcode));
        if (getDenatureTube() == null) {
            addValidationError("denatureTubeBarcode", "Could not find denature tube {0}", denatureTubeBarcode);
        }
    }

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
