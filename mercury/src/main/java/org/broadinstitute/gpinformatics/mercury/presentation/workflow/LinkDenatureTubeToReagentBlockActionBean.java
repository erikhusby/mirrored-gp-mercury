package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@UrlBinding("/workflow/LinkDenatureTubeToReagentBlock.action")
public class LinkDenatureTubeToReagentBlockActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/workflow/link_dtube_to_rb.jsp";

    @Validate(required = true, on = SAVE_ACTION)
    private String reagentBlockBarcode;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;
    @Inject
    private VesselTransferEjb vesselTransferEjb;
    @Inject
    private ProductOrderDao productOrderDao;

    @Validate(required = true, on = SAVE_ACTION)
    private String denatureTubeBarcode;
    private BarcodedTube denatureTube;

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

        BettaLIMSMessage bettaLIMSMessage = vesselTransferEjb
                .denatureToReagentKitTransfer(null, denatureMap, reagentBlockBarcode,
                        getUserBean().getLoginUserName(), "UI");
        bettaLimsMessageResource.processMessage(bettaLIMSMessage);

        addMessage("Denature Tube {0} associated with Reagent Block {1}", denatureTubeBarcode,
                reagentBlockBarcode);
        return new RedirectResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateData() {
        setDenatureTube((BarcodedTube) labVesselDao.findByIdentifier(denatureTubeBarcode));
        if (getDenatureTube() == null) {
            addValidationError("denatureTubeBarcode", "Could not find denature tube {0}", denatureTubeBarcode);
        }
    }

    public LabVessel getDenatureTube() {
        if (denatureTube == null && !denatureTubeBarcode.isEmpty()) {
            loadDenatureTube();
        }
        return denatureTube;
    }

    public void setDenatureTube(BarcodedTube denatureTube) {
        this.denatureTube = denatureTube;
    }

    public String getDenatureTubeBarcode() {
        return denatureTubeBarcode;
    }

    public void setDenatureTubeBarcode(String denatureTubeBarcode) {
        this.denatureTubeBarcode = denatureTubeBarcode;
    }

    @HandlesEvent("denatureInfo")
    public ForwardResolution denatureTubeInfo() {
        loadDenatureTube();
        return new ForwardResolution("/workflow/denature_tube_info.jsp");
    }

    private void loadDenatureTube() {
        denatureTube = (BarcodedTube) labVesselDao.findByIdentifier(denatureTubeBarcode);
    }
}
