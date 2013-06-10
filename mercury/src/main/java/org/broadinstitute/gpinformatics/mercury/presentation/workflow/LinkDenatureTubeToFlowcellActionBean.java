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

    @HandlesEvent("denatureInfo")
    public ForwardResolution denatureTubeInfo() {
        denatureTube = (TwoDBarcodedTube) labVesselDao.findByIdentifier(denatureTubeBarcode);
        if (denatureTube != null) {
            for (SampleInstance sample : denatureTube.getAllSamples()) {
                String productOrderKey = sample.getProductOrderKey();
                if (StringUtils.isNotEmpty(productOrderKey)) {
                    ProductOrder order = athenaClientService.retrieveProductOrderDetails(productOrderKey);
                    workflowName = order.getProduct().getWorkflowName();
                    break;
                }
            }
        }
        return new ForwardResolution("/workflow/denature_tube_info.jsp");
    }

    public ReceptaclePlateTransferEvent buildDenatureTubeToFlowcell(String eventType, String denatureTubeBarcode,
                                                                    String flowcellBarcode) {
        ReceptaclePlateTransferEvent event = new ReceptaclePlateTransferEvent();
        event.setEventType(eventType);
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            event.setStart(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        event.setDisambiguator(1L);
        event.setOperator(getContext().getUsername());
        event.setStation("UI");

        ReceptacleType denatureTube = new ReceptacleType();
        denatureTube.setBarcode(denatureTubeBarcode);
        denatureTube.setReceptacleType("tube");
        event.setSourceReceptacle(denatureTube);

        PlateType flowcell = new PlateType();
        flowcell.setBarcode(flowcellBarcode);
        flowcell.setPhysType(IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell.getAutomationName());
        flowcell.setSection(SBSSection.ALL2.getSectionName());
        event.setDestinationPlate(flowcell);

        return event;
    }
}
