package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.*;

@Named
@ConversationScoped
public class CreateBatchConversationData implements Serializable{

    @Inject
    private Conversation conversation;

    private LabVessel[] selectedVessels;

    private LabBatch batchObject;

    public LabVessel[] getSelectedVessels() {
        return selectedVessels;
    }

    public void setSelectedVessels(LabVessel[] selectedVessels) {

        this.selectedVessels = selectedVessels;
    }

    public void beginConversation() {
        if(conversation.isTransient()) {
            conversation.begin();
        }
    }

    public void endConversation() {
        conversation.end();
    }

    public void setBatchObject(LabBatch batchObject) {
        this.batchObject = batchObject;
    }

    public LabBatch getBatchObject() {
        return batchObject;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public List<String> getVesselLabels() {
        List<String> labels = new ArrayList<String>(selectedVessels.length);

        for(LabVessel vessel: selectedVessels) {
            labels.add(vessel.getLabel());
        }
        return labels;
    }

}
