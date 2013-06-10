package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.HashMap;
import java.util.Map;

@UrlBinding("/workflow/LinkDenatureTubeToReagentBlock.action")
public class LinkDenatureTubeToReagentBlockActionBean extends LinkDenatureTubeCoreActionBean {
    private static String VIEW_PAGE = "/workflow/link_dtube_to_rb.jsp";

    @Validate(required = true, on = SAVE_ACTION)
    private String reagentBlockBarcode;

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

        BettaLIMSMessage bettaLIMSMessage = vesselTransferBean
                .denatureToReagentKitTransfer(null, denatureMap, reagentBlockBarcode,
                        getUserBean().getLoginUserName(), "UI");
        bettalimsMessageResource.processMessage(bettaLIMSMessage);

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

}
