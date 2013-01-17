package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@UrlBinding(value = "/view/workflowView.action")
public class WorkflowViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/workflow/workflowView.jsp";

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    AthenaClientService athenaClientService;
    @Inject
    WorkflowLoader workflowLoader;

    private LabVessel vessel;

    private String vesselLabel;

    private LabEvent latestEvent;

    private Map<String, ProductWorkflowDefVersion> productWorkflowDefVersionMap = new HashMap<String, ProductWorkflowDefVersion>();

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
            ProductWorkflowDefVersion productWorkflowDefVersion = getWorkflowVersion(pdoKey);
            ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(pdoKey);
            productWorkflowDefVersionMap.put(productOrder.getProduct().getProductName(), productWorkflowDefVersion);
        }

        return new ForwardResolution(VIEW_PAGE);
    }

    public LabEvent getVesselEventByType(LabEventType type) {
        for (LabEvent event : vessel.getEvents()) {
            if (event.getLabEventType().equals(type)) {
                return event;
            }
        }
        return null;
    }

    public String getStepClass(WorkflowStepDef step) {
        LabEventType latestEventType = vessel.getLatestEvent().getLabEventType();
        for (LabEventType type : step.getLabEventTypes()) {
            LabEvent labEvent = getVesselEventByType(type);
            if (labEvent != null) {
                if (type.equals(latestEventType)) {
                    return "latest";
                } else {
                    return "latest";
                }
            }
        }
        return "";
    }

    public LabEvent getLastEventForStep(WorkflowStepDef step) {
        return getVesselEventByType(step.getLabEventTypes().get(step.getLabEventTypes().size() - 1));
    }

    private ProductWorkflowDefVersion getWorkflowVersion(String pdoKey) {
        WorkflowConfig workflowConfig = workflowLoader.load();
        ProductOrder productOrder = athenaClientService.retrieveProductOrderDetails(pdoKey);
        ProductWorkflowDef productWorkflowDef = workflowConfig.getWorkflowByName(
                productOrder.getProduct().getProductName());

        return productWorkflowDef.getEffectiveVersion();
    }
}
