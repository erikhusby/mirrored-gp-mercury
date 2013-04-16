package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;

@UrlBinding("/workflow/LinkDenatureTubeToFlowcell.action")
public class LinkDenatureTubeToFlowcellActionBean extends CoreActionBean {
    private static String VIEW_PAGE = "/resources/workflow/link_dtube_to_fc.jsp";

    @Inject
    BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private AthenaClientService athenaClientService;

    @Validate(required = true, on = SAVE_ACTION)
    private String denatureTubeBarcode;

    @Validate(required = true, on = SAVE_ACTION)
    private String flowcellBarcode;

    private TwoDBarcodedTube denatureTube;

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

    public void setDenatureTube(TwoDBarcodedTube denatureTube) {
        this.denatureTube = denatureTube;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public ForwardResolution showPage() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();

        ReceptaclePlateTransferEvent transferEventType = buildDenatureTubeToFlowcell(
                LabEventType.DENATURE_TO_FLOWCELL_TRANSFER.getName(), denatureTubeBarcode, flowcellBarcode);

        bettaLIMSMessage.getReceptaclePlateTransferEvent().add(transferEventType);
        bettalimsMessageResource.processMessage(bettaLIMSMessage);

        addMessage("Denature Tube {0} associated with Flowcell {1}", denatureTubeBarcode, flowcellBarcode);
        return new RedirectResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void validateData() {
        denatureTube = (TwoDBarcodedTube) labVesselDao.findByIdentifier(denatureTubeBarcode);
        if (denatureTube == null) {
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
        return new ForwardResolution("/resources/workflow/denature_tube_info.jsp");
    }

    public ReceptaclePlateTransferEvent buildDenatureTubeToFlowcell(String eventType, String denatureTubeBarcode, String flowcellBarcode) {
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
        flowcell.setSection(SBSSection.FLOWCELL2.getSectionName());
        event.setDestinationPlate(flowcell);

        return event;
    }
}
