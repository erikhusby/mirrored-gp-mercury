package org.broadinstitute.gpinformatics.mercury.control.workflow;

import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.infrastructure.common.SessionContextUtility;
import org.broadinstitute.gpinformatics.infrastructure.jmx.ExternalDataCacheControl;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * This class initializes WorkflowLoader so that it loads workflow config from
 * database Preferences when running in a CDI container. This class runs once
 * at startup after the container has finished its construction and injection.
 *
 * FYI the reason for this class: back when WorkflowLoader had injected preferenceDao
 * and sessionContextUtility I observed that CDI injection would not always happen.
 * For example in a JBoss deploy, WorkflowActionBean correctly injected a WorkflowLoader,
 * but the WorkflowLoader's injected preferenceDao and sessionContextUtility were null.
 * Then a few minutes later when WorkflowLoader had its periodic refresh via
 * ExternalDataCacheControl, then injection worked and the workflow config was loaded
 * from database preferences.
 * The working parts of WorkflowLoader are now static and the static fields are
 * initialized by this class.
 */
@Startup
@ApplicationScoped
public class WorkflowLoaderInit {
    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private SessionContextUtility sessionContextUtility;

    @Inject
    private ExternalDataCacheControl externalDataCacheControl;

    @PostConstruct
    private void postConstruct() {
        WorkflowLoader workflowLoader = WorkflowLoader.init(preferenceDao, sessionContextUtility);
        externalDataCacheControl.registerCache(workflowLoader);
    }
}
