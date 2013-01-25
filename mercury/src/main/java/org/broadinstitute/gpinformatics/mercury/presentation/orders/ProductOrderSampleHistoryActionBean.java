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
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDefVersion;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowProcessDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowStepDef;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.*;

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
    public Map<Integer, String> indexToStepNameMap = new TreeMap<Integer, String>();

    public Map<Integer, String> getIndexToStepNameMap() {
        return indexToStepNameMap;
    }

    public void setIndexToStepNameMap(Map<Integer, String> indexToStepNameMap) {
        this.indexToStepNameMap = indexToStepNameMap;
    }

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

        int count = 0;
        for (WorkflowProcessDef process : productWorkflowDefVersion.getWorkflowProcessDefs()) {
            for (WorkflowStepDef step : process.getEffectiveVersion().getWorkflowStepDefs()) {
                List<LabEventType> types = step.getLabEventTypes();
                for (LabEventType type : types) {
                    indexToStepNameMap.put(count, type.getName());
                    count++;
                }
            }
        }

        List<ProductOrderSample> samples = pdo.getSamples();
        List<String> sampleKeys = new ArrayList<String>();
        for (ProductOrderSample sample : samples) {
            sampleKeys.add(sample.getSampleName());
        }

        mercurySamples = mercurySampleDao.findBySampleKeys(sampleKeys);
        return new ForwardResolution(VIEW_PAGE);
    }

    public String getSparklineData(MercurySample sample) {
        Map<String, Set<LabEvent>> labEventsByName = getAllLabEvents(sample);
        StringBuilder seriesString = new StringBuilder();
        int count = 0;
        for (WorkflowProcessDef process : productWorkflowDefVersion.getWorkflowProcessDefs()) {
            for (WorkflowStepDef step : process.getEffectiveVersion().getWorkflowStepDefs()) {
                List<LabEventType> types = step.getLabEventTypes();
                for (LabEventType type : types) {
                    Set<LabEvent> stepEvents = labEventsByName.get(type.getName());
                    if (stepEvents != null) {
                        Integer repeatsRemoved = stepEvents.size() - step.getNumberOfRepeats();
                        seriesString.append(repeatsRemoved);
                    } else {
                        seriesString.append("0");
                    }
                    seriesString.append(",");
                    indexToStepNameMap.put(count, type.getName());
                    count++;
                }
            }
        }
        return seriesString.toString().substring(0, seriesString.length() - 2);
    }

    public Map<String, Set<LabEvent>> getAllLabEvents(MercurySample sample) {
        Map<String, Set<LabEvent>> labEventsByName = new HashMap<String, Set<LabEvent>>();
        List<LabVessel> vessels = labVesselDao.findBySampleKey(sample.getSampleKey());
        for (LabVessel vessel : vessels) {
            for (LabEvent event : vessel.getEvents()) {
                Set<LabEvent> eventList = labEventsByName.get(event.getLabEventType().getName());
                if (eventList == null) {
                    eventList = new HashSet<LabEvent>();
                    labEventsByName.put(event.getLabEventType().getName(), eventList);
                }
                eventList.add(event);

                //check descendent steps
                for (LabVessel descendantVessel : vessel.getDescendantVessels()) {
                    for (LabEvent descendantEvent : descendantVessel.getEvents()) {
                        eventList = labEventsByName.get(descendantEvent.getLabEventType().getName());
                        if (eventList == null) {
                            eventList = new HashSet<LabEvent>();
                            labEventsByName.put(descendantEvent.getLabEventType().getName(), eventList);
                        }
                        eventList.add(descendantEvent);
                    }
                }
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
            for (LabEvent event : vessel.getEvents()) {
                if (latestEvent == null) {
                    latestEvent = event;
                } else if (((Timestamp) event.getEventDate()).after(((Timestamp) latestEvent.getEventDate()))) {
                    latestEvent = event;
                }
            }
            //check descendent steps
            for (LabVessel descendantVessel : vessel.getDescendantVessels()) {
                for (LabEvent event : descendantVessel.getEvents()) {
                    if (latestEvent == null) {
                        latestEvent = event;
                    } else if (((Timestamp) event.getEventDate()).after(((Timestamp) latestEvent.getEventDate()))) {
                        latestEvent = event;
                    }
                }
            }
        }

        return latestEvent;
    }

    public WorkflowStepDef getLatestProcess(LabEvent event) {
        return productWorkflowDefVersion.findStepByEventType(event.getLabEventType().getName()).getStepDef();
    }

    public String getToolTipLookups() {
        StringBuilder tooltipLookups = new StringBuilder();
        for (Map.Entry entry : indexToStepNameMap.entrySet()) {
            tooltipLookups.append(entry.getKey());
            tooltipLookups.append(": '");
            tooltipLookups.append(entry.getValue());
            tooltipLookups.append("',");
            tooltipLookups.append("\n");
        }
        return tooltipLookups.toString();
    }


}
