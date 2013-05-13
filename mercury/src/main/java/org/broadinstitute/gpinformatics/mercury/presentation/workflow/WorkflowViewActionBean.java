package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@UrlBinding("/view/workflowView.action")
public class WorkflowViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/workflow/workflow.jsp";

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private AthenaClientService athenaClientService;
    @Inject
    private LabEventHandler labEventHandler;

    private LabVessel vessel;

    private String vesselLabel;

    private LabEvent latestEvent;

    private Map<String, ProductWorkflowDefVersion> productWorkflowDefVersionMap =
            new HashMap<String, ProductWorkflowDefVersion>();

    public LabVessel getVessel() {
        return vessel;
    }

    public void setVessel(LabVessel vessel) {
        this.vessel = vessel;
    }

    public LabEvent getLatestEvent() {
        return latestEvent;
    }

    public void setLatestEvent(LabEvent latestEvent) {
        this.latestEvent = latestEvent;
    }

    public Map<String, ProductWorkflowDefVersion> getProductWorkflowDefVersionMap() {
        return productWorkflowDefVersionMap;
    }

    public void setProductWorkflowDefVersionMap(Map<String, ProductWorkflowDefVersion> productWorkflowDefVersionMap) {
        this.productWorkflowDefVersionMap = productWorkflowDefVersionMap;
    }

    public String getVesselLabel() {
        return vesselLabel;
    }

    public void setVesselLabel(String vesselLabel) {
        this.vesselLabel = vesselLabel;
    }

    @DefaultHandler
    public Resolution view() {
        if (vesselLabel != null) {
            this.vessel = labVesselDao.findByIdentifier(vesselLabel);
        }
        Set<String> pdoKeys = vessel.getPdoKeys();
        for (String pdoKey : pdoKeys) {
            ProductWorkflowDefVersion productWorkflowDefVersion = labEventHandler.getWorkflowVersion(pdoKey);
            ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(pdoKey);
            productWorkflowDefVersionMap.put(productOrder.getProduct().getProductName(), productWorkflowDefVersion);
        }

        return new ForwardResolution(VIEW_PAGE);
    }

    public LabEvent getVesselEventByType(LabEventType type) {
        for (LabEvent event : vessel.getEvents()) {
            if (event.getLabEventType() == type) {
                return event;
            }
        }
        return null;
    }

    public String getStepClass(WorkflowStepDef step) {
        if (vessel.getLatestEvent() != null) {
            LabEventType latestEventType = vessel.getLatestEvent().getLabEventType();
            for (LabEventType type : step.getLabEventTypes()) {
                LabEvent labEvent = getVesselEventByType(type);
                if (labEvent != null) {
                    if (type == latestEventType) {
                        return "latest";
                    } else {
                        //change the class if we ever want to do different styling for current vs past steps.
                        return "latest";
                    }
                }
            }
        }
        return "";
    }

    public LabEvent getLastEventForStep(WorkflowStepDef step) {
        if (!step.getLabEventTypes().isEmpty()) {
            return getVesselEventByType(step.getLabEventTypes().get(step.getLabEventTypes().size() - 1));
        }
        return null;
    }

}
