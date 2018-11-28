package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.infrastructure.jmx.ExternalDataCacheControl;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
@ApplicationScoped
public class WorkflowLoader extends AbstractCache implements Serializable {

    // Valid for in-container only - DBFree tests will load workflow from file system
    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private SessionContextUtility sessionContextUtility;

    // Standalone only (DBFree tests)
    private static WorkflowConfig workflowConfigFromFile;

    private WorkflowConfig workflowConfig;

    public WorkflowLoader(){}

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
        if ( preferenceDao == null ) {
            // DB Free tests will load from file
            workflowConfig = loadFromFile();
        } else {
            sessionContextUtility.executeInContext(new SessionContextUtility.Function() {
                @Override
                public void apply() {
                    workflowConfig = loadFromPrefs();
                }
            });
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
