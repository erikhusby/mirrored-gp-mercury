package org.broadinstitute.gpinformatics.mercury.presentation.orders;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UrlBinding(value = "/view/pdoSampleHistory.action")
public class ProductOrderSampleHistoryActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/resources/orders/pdoSampleHistory.jsp";

    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private LabEventHandler labEventHandler;
    @Inject
    private AthenaClientService athenaClientService;

    private ProductWorkflowDefVersion productWorkflowDefVersion;
    private String businessKey;
    private List<MercurySample> mercurySamples;

    public List<MercurySample> getMercurySamples() {
        return mercurySamples;
    }

    public void setMercurySamples(List<MercurySample> mercurySamples) {
        this.mercurySamples = mercurySamples;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public ProductWorkflowDefVersion getProductWorkflowDefVersion() {
        return productWorkflowDefVersion;
    }

    public void setProductWorkflowDefVersion(ProductWorkflowDefVersion productWorkflowDefVersion) {
        this.productWorkflowDefVersion = productWorkflowDefVersion;
    }

    @DefaultHandler
    public Resolution view() {
        productWorkflowDefVersion = labEventHandler.getWorkflowVersion(businessKey);
        ProductOrder pdo = athenaClientService.retrieveProductOrderDetails(businessKey);
        List<ProductOrderSample> samples = pdo.getSamples();
        List<String> sampleKeys = new ArrayList<String>();
        for (ProductOrderSample sample : samples) {
            sampleKeys.add(sample.getSampleName());
        }

        mercurySamples = mercurySampleDao.findBySampleKeys(sampleKeys);
        return new ForwardResolution(VIEW_PAGE);
    }

    public Map<String, List<LabEvent>> getAllLabEvents(MercurySample sample) {
        Map<String, List<LabEvent>> labEventsByName = new HashMap<String, List<LabEvent>>();
        List<LabVessel> vessels = labVesselDao.findBySampleKey(sample.getSampleKey());
        for (LabVessel vessel : vessels) {
            for (LabEvent event : vessel.getEvents()) {
                List<LabEvent> eventList;
                if (labEventsByName.containsKey(event.getLabEventType().getName())) {
                    eventList = labEventsByName.get(event.getLabEventType().getName());
                } else {
                    eventList = new ArrayList<LabEvent>();
                }
                eventList.add(event);
                labEventsByName.put(event.getLabEventType().getName(), eventList);
            }
        }
        return labEventsByName;
    }

    public LabEvent getLatestLabEvent(MercurySample sample) {
        LabEvent latestEvent = null;
        List<LabVessel> vessels = labVesselDao.findBySampleKey(sample.getSampleKey());
        List<LabVessel> targetVessels = new ArrayList<LabVessel>();

        //check this vessels steps
        for (LabVessel vessel : vessels) {
            targetVessels.addAll(vessel.getDescendantVessels());
            for (LabEvent event : vessel.getEvents()) {
                if (latestEvent == null) {
                    latestEvent = event;
                } else if (latestEvent.getEventDate().before(event.getEventDate())) {
                    latestEvent = event;
                }
            }
        }
        //check descendent steps
        for (LabVessel vessel : targetVessels) {
            for (LabEvent event : vessel.getEvents()) {
                if (latestEvent == null) {
                    latestEvent = event;
                } else if (((Timestamp)event.getEventDate()).after(((Timestamp)latestEvent.getEventDate()))){
                    latestEvent = event;
                }
            }
        }

        return latestEvent;
    }

    public WorkflowStepDef getLatestProcess(LabEvent event) {
        return productWorkflowDefVersion.findStepByEventType(event.getLabEventType().getName()).getStepDef();
    }
}
