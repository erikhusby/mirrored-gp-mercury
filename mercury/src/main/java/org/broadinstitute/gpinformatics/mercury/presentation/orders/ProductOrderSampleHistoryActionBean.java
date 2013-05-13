package org.broadinstitute.gpinformatics.mercury.presentation.orders;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.time.DurationFormatUtils;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@UrlBinding(value = "/view/pdoSampleHistory.action")
public class ProductOrderSampleHistoryActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/orders/pdo_sample_history.jsp";

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
    private Set<MercurySample> mercurySamples = new HashSet<MercurySample>();
    public Map<Integer, String> indexToStepNameMap = new TreeMap<Integer, String>();

    public Map<Integer, String> getIndexToStepNameMap() {
        return indexToStepNameMap;
    }

    public void setIndexToStepNameMap(Map<Integer, String> indexToStepNameMap) {
        this.indexToStepNameMap = indexToStepNameMap;
    }

    public Set<MercurySample> getMercurySamples() {
        return mercurySamples;
    }

    public void setMercurySamples(Set<MercurySample> mercurySamples) {
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
        if (productWorkflowDefVersion != null) {
            for (WorkflowProcessDef process : productWorkflowDefVersion.getWorkflowProcessDefs()) {
                for (WorkflowStepDef step : process.getEffectiveVersion().getWorkflowStepDefs()) {
                    List<LabEventType> types = step.getLabEventTypes();
                    for (LabEventType type : types) {
                        indexToStepNameMap.put(count, type.getName());
                        count++;
                    }
                }
            }
            mercurySamples
                    .addAll(mercurySampleDao.findBySampleKeys(ProductOrderSample.getSampleNames(pdo.getSamples())));
        }

        return new ForwardResolution(VIEW_PAGE);
    }

    public String getSparklineData(MercurySample sample) {
        Map<String, Set<LabEvent>> labEventsByName = getAllLabEvents(sample);
        StringBuilder seriesString = new StringBuilder();
        for (WorkflowProcessDef process : productWorkflowDefVersion.getWorkflowProcessDefs()) {
            for (WorkflowStepDef step : process.getEffectiveVersion().getWorkflowStepDefs()) {
                List<LabEventType> types = step.getLabEventTypes();
                for (LabEventType type : types) {
                    Set<LabEvent> stepEvents = labEventsByName.get(type.getName());
                    if (stepEvents != null) {
                        int repeatsRemoved = stepEvents.size() - step.getNumberOfRepeats();
                        seriesString.append(repeatsRemoved);
                    } else {
                        seriesString.append("0");
                    }
                    seriesString.append(",");
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

    public LabEvent getFirstLabEvent(MercurySample sample) {
        LabEvent latestEvent = null;
        List<LabVessel> vessels = labVesselDao.findBySampleKey(sample.getSampleKey());

        //check this vessels steps
        for (LabVessel vessel : vessels) {
            for (LabEvent event : vessel.getEvents()) {
                if (latestEvent == null) {
                    latestEvent = event;
                } else if (((Timestamp) event.getEventDate()).before(((Timestamp) latestEvent.getEventDate()))) {
                    latestEvent = event;
                }
            }
            //check descendent steps
            for (LabVessel descendantVessel : vessel.getDescendantVessels()) {
                for (LabEvent event : descendantVessel.getEvents()) {
                    if (latestEvent == null) {
                        latestEvent = event;
                    } else if (((Timestamp) event.getEventDate()).before(((Timestamp) latestEvent.getEventDate()))) {
                        latestEvent = event;
                    }
                }
            }
        }

        return latestEvent;
    }

    public String getDuration(MercurySample sample) {
        LabEvent firstLabEvent = getFirstLabEvent(sample);
        LabEvent latestLabEvent = getLatestLabEvent(sample);
        Long mSecDiff = 0l;
        if (firstLabEvent != null && latestLabEvent != null) {
            mSecDiff = latestLabEvent.getEventDate().getTime() - firstLabEvent.getEventDate().getTime();
        }
        return DurationFormatUtils.formatDurationWords(mSecDiff, true, false);
    }

    public WorkflowStepDef getLatestProcess(LabEvent event) {
        ProductWorkflowDefVersion.LabEventNode eventNode;
        if (event != null) {
            eventNode = productWorkflowDefVersion.findStepByEventType(event.getLabEventType().getName());
            if (eventNode != null) {
                return eventNode.getStepDef();
            } else {
                return null;
            }
        }
        return null;
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
