package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;

/**
 * This class holds the most recent copy of WorkflowConfig, loaded from either the database
 * or from a file when doing database-free tests.
 */
public class WorkflowLoader extends AbstractCache implements Serializable {
    private static PreferenceDao preferenceDao;
    private static SessionContextUtility sessionContextUtility;
    private static WorkflowConfig workflowConfig;

    private WorkflowLoader() {
    }

    /** One-time initialization is done by calling this when the CDI container starts up. */
    static WorkflowLoader init(PreferenceDao preferenceDao, SessionContextUtility sessionContextUtility) {
        WorkflowLoader.preferenceDao = preferenceDao;
        WorkflowLoader.sessionContextUtility = sessionContextUtility;
        sessionContextUtility.executeInContext(() -> loadFromPrefs());
        return new WorkflowLoader();
    }

    public static WorkflowConfig getWorkflowConfig() {
        // workflowConfig will be null only when container-free unit tests are run.
        if (workflowConfig == null) {
            workflowConfig = loadFromFile();
        }
        return workflowConfig;
    }

    /**
     * Periodically reloads the WorkflowConfig from the database.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void refreshCache() {
        if (sessionContextUtility != null) {
            sessionContextUtility.executeInContext(() -> loadFromPrefs());
        }
    }

    /**
     * Pulls workflow configuration from preference xml in the database.
     */
    private static void loadFromPrefs() {
        Preference workflowConfigPref = preferenceDao.getGlobalPreference(PreferenceType.WORKFLOW_CONFIGURATION);
        if (workflowConfigPref == null) {
            throw new RuntimeException("Failed to retrieve WORKFLOW_CONFIGURATION preference");
        }
        try {
            workflowConfig = (WorkflowConfig) PreferenceType.WORKFLOW_CONFIGURATION.getCreator()
                    .create(workflowConfigPref.getData());
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *  Pull workflow configuration from file when running outside a container
     */
    public static WorkflowConfig loadFromFile() {
        try {
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            return (WorkflowConfig) unmarshaller.unmarshal(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
