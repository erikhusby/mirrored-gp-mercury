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
import javax.enterprise.context.ApplicationScoped;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;

/**
 * This class holds the most recent copy of WorkflowConfig, loaded from either the database
 * or from a file when doing database-free tests.
 */
@ApplicationScoped
public class WorkflowLoader extends AbstractCache implements Serializable {
    private static PreferenceDao preferenceDao;
    private static SessionContextUtility sessionContextUtility;
    private static WorkflowConfig workflowConfig;

    public WorkflowLoader() {
    }

    /** One-time initialization is done by calling this when the CDI container starts up. */
    static WorkflowLoader init(PreferenceDao preferenceDao, SessionContextUtility sessionContextUtility) {
        WorkflowLoader.preferenceDao = preferenceDao;
        WorkflowLoader.sessionContextUtility = sessionContextUtility;
        sessionContextUtility.executeInContext(() -> loadFromPrefs());
        return new WorkflowLoader();
    }

    public static WorkflowConfig getWorkflowConfig() {
        if (workflowConfig == null) {
            // workflowConfig should only be null when dbFree tests are run, and it's loaded from a file.
            loadFromFile();
        }
        return workflowConfig;
    }

    /**
     * Periodically reloads the WorkflowConfig from the database. Synchronization is expected to be
     * provided by the container since this class is annotated ApplicationScoped.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void refreshCache() {
        if (sessionContextUtility != null) {
            System.out.println("WorkflowLoader refresh xxx");
            sessionContextUtility.executeInContext(() -> loadFromPrefs());
        }
    }

    /**
     * Pulls workflow configuration from preference xml in the database.
     */
    private static int prefCount = 0;//xxx
    private static void loadFromPrefs() {
        if (prefCount++ % 10 == 0) System.out.println(prefCount + " loads from db xxx");
        Preference workflowConfigPref = preferenceDao.getGlobalPreference(PreferenceType.WORKFLOW_CONFIGURATION);
        if (workflowConfigPref == null) {
            throw new RuntimeException("Failed to retreive WORKFLOW_CONFIGURATION preference");
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
    private static int fileCount = 0;//xxx
    private static void loadFromFile() {
        if (fileCount++ % 10 == 0) System.out.println(fileCount + " loads from file xxx");
        try {
            JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            workflowConfig = (WorkflowConfig) unmarshaller.unmarshal(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
