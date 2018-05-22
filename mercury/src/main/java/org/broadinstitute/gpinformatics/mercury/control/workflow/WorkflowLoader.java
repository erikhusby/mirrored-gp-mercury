package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.infrastructure.jmx.ExternalDataCacheControl;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Serializable;

/**
 * This class is responsible for loading and refreshing the WorkflowConfiguration from the database. For database-free
 * tests, the configuration is read from the filesystem. As WorkflowLoader extends AbstractCache, the cached
 * WorkflowConfig is periodically refreshed.
 *
 * @see ExternalDataCacheControl#invalidateCache()
 */
@Singleton
public class WorkflowLoader extends AbstractCache implements Serializable {
    private final Object lock = new Object();

    public static boolean IS_INITIALIZED = false;

    /**
     * Defaults to using file based configuration access for DBFree testing <br/>
     * This state is only changed to true via EJB lifecycle methods, DBFree has no effect
     */
    private static boolean IS_STANDALONE = true;

    // Valid for in-container only - DBFree tests will load workflow from file system
    @Inject
    private PreferenceDao preferenceDao;

    // Standalone only (DBFree tests)
    private static WorkflowConfig workflowConfigFromFile;

    private WorkflowConfig workflowConfig;

    public WorkflowLoader(){}

    /**
     * EJB lifecycle method sets state of cache to use database (vs. filesystem for DBFree testing)
     */
    @PostConstruct
    public void initialize(){
        // Use availability of context to test for running in container
        try{
            new InitialContext().lookup("java:comp/env");
            IS_STANDALONE = false;
        } catch (NamingException e) {
            IS_STANDALONE = true;
        }
        IS_INITIALIZED = true;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public WorkflowConfig load() {
        if (workflowConfig==null){
            refreshCache();
        }
        return workflowConfig;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void refreshCache() {

        // Sequence of @PostConstruct calls between superclass and this results in this method getting called before
        // @PostConstruct initialize() method
        if( !IS_INITIALIZED ) {
            initialize();
        }

        synchronized (lock) {
            if (IS_STANDALONE) {
                workflowConfig = loadFromFile();
            } else {
                workflowConfig = loadFromPrefs();
            }
        }
    }

    /**
     * Pull workflow configuration from preferences when running in container
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

    /**
     *  Pull workflow configuration from file when running outside a container
     */
    private WorkflowConfig loadFromFile() {
            try {
                JAXBContext jc = JAXBContext.newInstance(WorkflowConfig.class, WorkflowBucketDef.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();
                workflowConfigFromFile = (WorkflowConfig) unmarshaller.unmarshal(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream("WorkflowConfig.xml"));
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        return workflowConfigFromFile;
    }

}
