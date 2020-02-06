package org.broadinstitute.gpinformatics.mercury.boundary.storage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestConfig;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import java.util.Date;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Singleton that runs a periodic timer to renew a Google service account credential.
 */
@Startup
@Singleton
@ConcurrencyManagement(BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class GoogleCredentialRenewer {
    private Log log = LogFactory.getLog(getClass().getSimpleName());
    private Date previousNextTimeout = new Date(0);

    @Inject
    private Deployment deployment;

    @Resource
    TimerService mayoCredentialTimer;

    @Inject
    private GoogleBucketDao googleBucketDao;

    @PostConstruct
    public void initialize() {
        // Renews the Mayo bucket credential once a day.
        mayoCredentialTimer.createCalendarTimer(new ScheduleExpression().hour("20"),
                new TimerConfig("MayoCredentialTimer", false));
    }

    @Timeout
    void timeout(Timer timer) {
        // Skips retries, indicated by a repeated nextTimeout value.
        Date nextTimeout = timer.getNextTimeout();
        if (nextTimeout.after(previousNextTimeout)) {
            previousNextTimeout = nextTimeout;

            MayoManifestConfig mayoManifestConfig = (MayoManifestConfig) MercuryConfiguration.getInstance().
                    getConfig(MayoManifestConfig.class, deployment);
            googleBucketDao.setConfigGoogleStorageConfig(mayoManifestConfig);

            // The yaml config parameter must be set to true in order for this to run.
            if ("true".equals(mayoManifestConfig.getDailyCredentialRenewal())) {
                MessageCollection messageCollection = new MessageCollection();
                googleBucketDao.rotateServiceAccountKey(messageCollection);

                if (messageCollection.hasErrors()) {
                    log.error("Cannot renew Mayo manifest bucket credential.");
                    messageCollection.getErrors().forEach(message -> log.error(message));
                }
            }
        }
    }
}