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

import javax.ejb.Stateless;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Stateless
@Dependent
public class CovidIntakeEjb {
    // Covid intake files in the Google bucket must be named starting with this string,
    // otherwise they are ignored.
    private static final String COVID_FILE_PREFIX = "covid_";
    private static final Log log = LogFactory.getLog(CovidIntakeEjb.class);
    private CovidIntakeBucketConfig config;

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

    /** CDI constructor. */
    @SuppressWarnings("UnusedDeclaration")
    public CovidIntakeEjb() {}

    /**
     * Polls the google bucket looking for unprocessed files.
     * For each new file, parses the spreadsheet and accessions the tube and mercury sample.
     */
    public void pollAndAccession() {
        config = (CovidIntakeBucketConfig) MercuryConfiguration.getInstance().
                getConfig(CovidIntakeBucketConfig.class, deployment);

        googleBucketDao.setConfigGoogleStorageConfig(config);
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
     * Gets a listing of all files in the google bucket and removes all of the the filenames
     * that have already been processed.
     */
    List<String> findNewFiles() {
        MessageCollection messageCollection = new MessageCollection();
        List<String> listing = googleBucketDao.list(messageCollection).stream().
                filter(filename -> filename.startsWith(COVID_FILE_PREFIX) ||
                        filename.contains("/" + COVID_FILE_PREFIX)).
                collect(Collectors.toList());
        logErrorsAndWarns(messageCollection);
        listing.removeAll(labVesselDao.findListByList(ManifestFile.class, ManifestFile_.filename, listing).stream().
                map(ManifestFile::getFilename).collect(Collectors.toList()));
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