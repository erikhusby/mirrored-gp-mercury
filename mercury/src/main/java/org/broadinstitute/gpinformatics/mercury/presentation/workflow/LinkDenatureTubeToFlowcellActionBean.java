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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

@UrlBinding("/workflow/LinkDenatureTubeToFlowcell.action")
public class LinkDenatureTubeToFlowcellActionBean extends LinkDenatureTubeCoreActionBean {
    private static String VIEW_PAGE = "/workflow/link_dtube_to_fc.jsp";

    @Validate(required = true, on = SAVE_ACTION)
    private String flowcellBarcode;

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public void setFlowcellBarcode(String flowcellBarcode) {
        this.flowcellBarcode = flowcellBarcode;
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public ForwardResolution showPage() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();

        ReceptaclePlateTransferEvent transferEventType = vesselTransferBean.buildDenatureTubeToFlowcell(
                LabEventType.DENATURE_TO_FLOWCELL_TRANSFER.getName(), denatureTubeBarcode, flowcellBarcode,
                getUserBean().getLoginUserName(), "UI");

        bettaLIMSMessage.getReceptaclePlateTransferEvent().add(transferEventType);
        bettalimsMessageResource.processMessage(bettaLIMSMessage);

        addMessage("Denature Tube {0} associated with Flowcell {1}", denatureTubeBarcode, flowcellBarcode);
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
