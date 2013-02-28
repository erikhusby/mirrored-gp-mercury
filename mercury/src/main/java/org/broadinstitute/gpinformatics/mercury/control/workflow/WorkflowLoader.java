package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;

/**
 * Loads a workflow configuration, initially from the file system
 */
public class WorkflowLoader implements Serializable {
    private static WorkflowConfig workflowConfig;

    public WorkflowConfig load() {
        if (workflowConfig == null) {
            try {
                JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                workflowConfig = (WorkflowConfig) unmarshaller.unmarshal(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return workflowConfig;
    }
}
