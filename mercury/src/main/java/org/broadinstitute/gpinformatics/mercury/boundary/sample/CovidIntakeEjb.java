package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.ManifestFile;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.ManifestFile_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

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
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@Startup
@Singleton
@ConcurrencyManagement(BEAN)
public class CovidIntakeEjb {
    private static final Log log = LogFactory.getLog(CovidIntakeEjb.class);

    // Covid intake files in the Google bucket must be named starting with this string,
    // otherwise they are ignored.
    private static final String COVID_FILE_PREFIX = "covid_";

    @Inject
    private GoogleBucketDao googleBucketDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private UserBean userBean;
    @Inject
    private Deployment deployment;

    @Resource
    private TimerService timerService;
    private static Date previousNextTimeout = new Date(0);
    private final String timerName = "CovidIntake timer";

    /**
     * Interval in minutes for the timer to fire off.
     */
    private int timerPeriod = 1;

    /** CDI constructor. */
    @SuppressWarnings("UnusedDeclaration")
    public CovidIntakeEjb() {}

    @PostConstruct
    public void postConstruct() {
        // Gets the yaml file config.
        CovidIntakeBucketConfig config = (CovidIntakeBucketConfig) MercuryConfiguration.getInstance().
                getConfig(CovidIntakeBucketConfig.class, deployment);

        // Sets up the period timer if bucket access is configured.
        if (config != null &&
                StringUtils.isNotBlank(config.getBucketName()) &&
                StringUtils.isNotBlank(config.getCredentialFilename())) {
            googleBucketDao.setConfigGoogleStorageConfig(config);
            ScheduleExpression expression = new ScheduleExpression();
            expression.minute("*/" + timerPeriod).hour("*");
            timerService.createCalendarTimer(expression, new TimerConfig(timerName, false));
        }
    }

    @Timeout
    void timeout(Timer timer) {
        // Skips retries, indicated by a repeated nextTimeout value.
        Date nextTimeout = timer.getNextTimeout();
        if (nextTimeout.after(previousNextTimeout)) {
            previousNextTimeout = nextTimeout;
            pollAndAccession();
        } else {
            log.trace("Skipping retry of " + timerName);
        }
    }

    /**
     * Polls the google bucket looking for unprocessed files.
     * For each new file, parses the spreadsheet and accessions the tube and mercury sample.
     */
    public void pollAndAccession() {
        List<String> filenames = findNewFiles();
        if (!filenames.isEmpty()) {
            userBean.login("seqsystem");
            for (String filename : filenames) {
                byte[] content = download(filename);
                CovidIntakeParser covidIntakeParser = new CovidIntakeParser(content, filename);
                covidIntakeParser.parse();
                makeTubesAndSamples(covidIntakeParser.getDtos());
                labVesselDao.persist(new ManifestFile(filename));
            }
        }
    }

    /**
     * Creates/updates tube and sample.
     */
    private void makeTubesAndSamples(List<CovidIntakeParser.Dto> dtos) {
        Map<String, LabVessel> tubes = labVesselDao.findByBarcodes(dtos.stream().
                map(CovidIntakeParser.Dto::getLabel).
                collect(Collectors.toList()));
        Map<String, MercurySample> samples = mercurySampleDao.findMapIdToMercurySample(dtos.stream().
                map(CovidIntakeParser.Dto::getSampleName).
                collect(Collectors.toList()));
        for (CovidIntakeParser.Dto dto : dtos) {
            LabVessel labVessel = tubes.get(dto.getLabel());
            if (labVessel == null) {
                labVessel = new BarcodedTube(dto.getLabel(), BarcodedTube.BarcodedTubeType.MatrixTube);
                labVesselDao.persist(labVessel);
            } else {
                // Logs the error if the tube already has multiple samples or one that doesn't match
                // the intake manifest. Tube and sample are made to match the intake manifest.
                if (labVessel.getMercurySamples().size() > 1 ||
                        !labVessel.getMercurySamples().iterator().next().getSampleKey().equals(dto.getSampleName())) {
                    log.error("Covid tube " + labVessel.getLabel() +
                            " is already in Mercury and contains unexpected sample " +
                            labVessel.getMercurySamples().stream().map(MercurySample::getSampleKey).
                                    sorted().collect(Collectors.joining(",")) +
                            " and will be reset to have sample " + dto.getSampleName());
                    labVessel.clearSamples();
                }
            }
            Set<Metadata> sampleMetadata = dto.getSampleMetadata().entrySet().stream().
                    map(mapEntry -> new Metadata(mapEntry.getKey(), mapEntry.getValue())).
                    collect(Collectors.toSet());
            MercurySample sample = samples.get(dto.getSampleName());
            if (sample == null) {
                sample = new MercurySample(dto.getSampleName(), MercurySample.MetadataSource.MERCURY);
                labVesselDao.persist(sample);
            } else {
                List<String> intakeMetadata =
                        sampleMetadata.stream().map(Metadata::toString).sorted().collect(Collectors.toList());
                List<String> existingMetadata =
                        sample.getMetadata().stream().map(Metadata::toString).sorted().collect(Collectors.toList());
                intakeMetadata.removeAll(existingMetadata);
                if (!intakeMetadata.isEmpty()) {
                    log.error("Covid sample " + sample.getSampleKey() +
                            " is already in Mercury and contains unexpected metadata " +
                            labVessel.getMercurySamples().stream().map(MercurySample::getSampleKey).
                                    sorted().collect(Collectors.joining(",")) +
                            " and will be reset to match the intake manifest.");
                    sample.getMetadata().clear();
                }
            }
            sample.addMetadata(sampleMetadata);
            labVessel.addSample(sample);
        }
    }

    /**
     * Returns the contents of the Google bucket file.
     */
    private byte[] download(String filename) {
        MessageCollection messageCollection = new MessageCollection();
        if (googleBucketDao.exists(filename, messageCollection)) {
            return googleBucketDao.download(filename, messageCollection);
        }
        logErrorsAndWarns(messageCollection);
        log.error("Covid file " + filename + " cannot be read.");
        return null;
    }

    /**
     * Gets a listing of all files in the google bucket and removes the filenames
     * that have already been processed.
     */
    List<String> findNewFiles() {
        MessageCollection messageCollection = new MessageCollection();
        List<String> listing = googleBucketDao.list(messageCollection).stream().
                filter(filename -> filename.startsWith(COVID_FILE_PREFIX) ||
                        filename.contains("/" + COVID_FILE_PREFIX)).
                collect(Collectors.toList());
        logErrorsAndWarns(messageCollection);
        listing.removeAll(labVesselDao.findListByList(ManifestFile.class, ManifestFile_.filename, listing));
        return listing;
    }

    private void logErrorsAndWarns(MessageCollection messageCollection) {
        String errors = StringUtils.join(messageCollection.getErrors(), " ; ");
        if (!errors.isEmpty()) {
            log.error(errors);
        }
        String warnings = StringUtils.join(messageCollection.getWarnings(), " ; ");
        if (!warnings.isEmpty()) {
            log.warn(warnings);
        }

    }
}