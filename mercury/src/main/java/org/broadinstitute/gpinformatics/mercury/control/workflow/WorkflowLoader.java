package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;

/**
 * Loads a workflow configuration from the file system if outside a container (DBFree tests)
 *  or from preference table if called from within a container (Deployed app and Arquillian tests)
 */
@Stateful
@RequestScoped
public class WorkflowLoader implements Serializable {

    // Use file based configuration access for DBFree testing
    private static boolean IS_STANDALONE = false;

    // Valid for in-container only - DBFree tests will load workflow from file system
    @Inject
    private PreferenceDao preferenceDao;

    // Standalone only (DBFree tests)
    private static WorkflowConfig workflowConfigFromFile;

    // Use availability of context to test for running in container
    static {
        try{
            new InitialContext().lookup("java:comp/env");
        } catch (NamingException e) {
            IS_STANDALONE = true;
        }
    }

    public WorkflowLoader(){}

    public WorkflowConfig load() {
        if( IS_STANDALONE ) {
            return loadFromFile();
        } else {
            return loadFromPrefs();
        }
    }

    /**
     * Pull workflow configuration from preferences when running in container
     * TODO JMS This is an expensive reload for rarely changing data.  A 30 minute cache would be nice...
     * @return
     */
    private WorkflowConfig loadFromPrefs() {

        if( preferenceDao == null ) {
            throw new RuntimeException("Attempt to load preferences from DB without JavaEE container services.");
        }

        Preference workflowConfigPref = preferenceDao.getGlobalPreference(PreferenceType.WORKFLOW_CONFIGURATION);
        if( workflowConfigPref == null ) {
            throw new RuntimeException("Failed to retreive WORKFLOW_CONFIGURATION preference");
        }

        WorkflowConfig config = null;

        try {
            config = (WorkflowConfig) PreferenceType.WORKFLOW_CONFIGURATION.getCreator().create(workflowConfigPref.getData());
        } catch ( RuntimeException re ){
            throw re;
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        }

        return config;
    }

    /** Pull workflow configuration from file when running outside a container
     *
     * @return
     */
    private WorkflowConfig loadFromFile() {
        if (workflowConfigFromFile == null) {
            try {
                JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                workflowConfigFromFile = (WorkflowConfig) unmarshaller.unmarshal(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return workflowConfigFromFile;
    }
}
