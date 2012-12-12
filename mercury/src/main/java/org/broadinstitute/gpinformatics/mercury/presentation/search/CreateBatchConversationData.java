package org.broadinstitute.gpinformatics.mercury.presentation.search;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@ConversationScoped
public class CreateBatchConversationData implements Serializable{

    @Inject
    private Conversation conversation;

    private String      jiraKey;
    private LabVessel[] selectedVessels;

    private LabBatch batchObject;

    public LabVessel[] getSelectedVessels() {
        return selectedVessels;
    }

    public String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(String jiraKey) {
        this.jiraKey = jiraKey;
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

}
