package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferBean;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@UrlBinding("/workflow/LinkDenatureTubeToReagentBlock.action")
public class LinkDenatureTubeToReagentBlockActionBean extends LinkDenatureTubeCoreActionBean {
    private static String VIEW_PAGE = "/workflow/link_dtube_to_rb.jsp";

    @Inject
    VesselTransferBean vesselTransferBean;

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
        denatureMap.put(getDenatureTubeBarcode(), VesselPosition.A01);
        LabEvent transferEventType = vesselTransferBean
                .denatureToReagentKitTransfer(null, denatureMap, reagentBlockBarcode,
                        getUserBean().getLoginUserName(), "UI");

        addMessage("Denature Tube {0} associated with Reagent Block {1}", getDenatureTubeBarcode(),
                reagentBlockBarcode);
        return new RedirectResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateData() {
        setDenatureTube((TwoDBarcodedTube) getLabVesselDao().findByIdentifier(getDenatureTubeBarcode()));
        if (getDenatureTube() == null) {
            addValidationError("denatureTubeBarcode", "Could not find denature tube {0}", getDenatureTubeBarcode());
        }
    }

}
