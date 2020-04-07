package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.base.Objects;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.GoogleBucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.receiving.ColorCovidReceiptActionBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Stateful
@RequestScoped
public class ColorCovidManifestEjb {
    @Inject
    private GoogleBucketDao googleBucketDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private Deployment deployment;
    @Inject
    private TubeFormationDao tubeFormationDao;
    @Inject
    private CovidManifestCopier covidManifestCopier;

    private static final String WRONG_TUBE_IN_POSITION = "At position %s the rack has %s but manifest shows %s.";
    private static final BarcodedTube.BarcodedTubeType DEFAULT_TUBE_TYPE = BarcodedTube.BarcodedTubeType.MatrixTube;
    // Assumes every rack is a 96 tube Matrix rack.
    private static final RackOfTubes.RackType DEFAULT_RACK_TYPE = RackOfTubes.RackType.Matrix96;

    /**
     * CDI constructor.
     */
    @SuppressWarnings("UnusedDeclaration")
    public ColorCovidManifestEjb() {
    }

    /**
     * Validates the rack scan. Neither the rack nor the tubes are expected to exist yet.
     * A manifest file must be found for the rack barcode, and the rack scan tubes must
     * match the manifest (positions too, if they're in the manifest).
     */
    public void validate(ColorCovidReceiptActionBean bean) {
        MessageCollection messages = bean.getMessageCollection();
        final String rackBarcode = bean.getRackBarcode();
        // Cannot receive an existing rack.
        LabVessel rack = labVesselDao.findByIdentifier(rackBarcode);
        if (rack != null) {
            messages.addError("Rack " + rackBarcode + " already exists in Mercury.");
            return;
        }

        // Disallows vessels that were already accessioned.
        List<String> vesselLabels = bean.getRackScan().values().stream().
                filter(StringUtils::isNotBlank).
                collect(Collectors.toList());
        vesselLabels.add(0, bean.getRackBarcode());
        String existingVessels = labVesselDao.findByBarcodes(vesselLabels).entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() != null).
                map(Map.Entry::getKey).
                sorted().
                collect(Collectors.joining(" "));
        if (!existingVessels.isEmpty()) {
            messages.addError("Tube(s) already exists in Mercury and cannot be re-received: " + existingVessels);
            return;
        }

        // The rack scan positions must all be valid for the rack type.
        String invalidPositions = CollectionUtils.subtract(
                bean.getRackScan().keySet(),
                Stream.of(DEFAULT_RACK_TYPE.getVesselGeometry().getVesselPositions()).
                        map(VesselPosition::name).
                        collect(Collectors.toList())).
                stream().sorted().collect(Collectors.joining(" "));
        if (!invalidPositions.isEmpty()) {
            messages.addError("Rack scan has positions not valid for " + DEFAULT_RACK_TYPE.getDisplayName() +
                    ": " + invalidPositions);
            return;
        }

        // Finds the manifest file.
        googleBucketDao.setConfigGoogleStorageConfig((ColorCovidManifestConfig) MercuryConfiguration.getInstance().
                getConfig(ColorCovidManifestConfig.class, deployment));
        // Looks for a file named with the plain rack barcode, then with .csv, then with .CSV.
        String[] filenames = {rackBarcode, rackBarcode + ".csv", rackBarcode + ".CSV"};
        String filename = Stream.of(filenames).
                filter(name -> googleBucketDao.exists(name, messages)).
                findFirst().orElse(null);
        if (filename == null) {
            messages.addError("Cannot find manifest file " + StringUtils.join(filenames, " or "));
            return;
        }
        messages.addInfo("Found manifest '" + filename + "'.");
        bean.setFilename(filename);

        // Parses the manifest into a list of dtos.
        byte[] content = null;
        try {
            content = googleBucketDao.download(filename, messages);
        } catch (Exception e) {
            messages.addError("Failed to download manifest file " + filename, e);
            return;
        }
        ColorCovidManifestParser colorCovidManifestParser = new ColorCovidManifestParser(content, filename);
        colorCovidManifestParser.parse(messages);
        if (messages.hasErrors()) {
            return;
        }
        List<ColorCovidManifestParser.Dto> dtos = colorCovidManifestParser.getDtos();

        // Rack scan and manifest must agree.
        List<String> rackScanBarcodes = bean.getRackScan().values().stream().
                filter(StringUtils::isNotBlank).
                collect(Collectors.toList());
        // Uses positions if manifest supplies them. The parser will have errored if they are inconsistently supplied.
        if (StringUtils.isNotBlank(dtos.get(0).getSampleMetadata().get(Metadata.Key.WELL_POSITION))) {
            Map<String, ColorCovidManifestParser.Dto> positionToDto = dtos.stream().
                    collect(Collectors.toMap(dto -> dto.getSampleMetadata().get(Metadata.Key.WELL_POSITION),
                            Function.identity()));
            // Iterates on the non-blank positions from both dtos and the manifest.
            Stream.concat(positionToDto.keySet().stream(), bean.getRackScan().keySet().stream()).
                    filter(StringUtils::isNotBlank).
                    distinct().
                    forEach(position -> {
                        String manifestTube = positionToDto.get(position) == null ?
                                null : positionToDto.get(position).getLabel();
                        String rackTube = bean.getRackScan().get(position);
                        if (StringUtils.isNotBlank(manifestTube) && StringUtils.isNotBlank(rackTube) &&
                                !rackTube.equals(manifestTube)) {
                            messages.addError(String.format(WRONG_TUBE_IN_POSITION, position,
                                    StringUtils.isBlank(rackTube) ? "no tube" : rackTube,
                                    StringUtils.isBlank(manifestTube) ? "no tube" : manifestTube));
                        }
                    });
        } else {
            // For manifests without position info, just compares tube barcodes.
            List<String> manifestTubeBarcodes = dtos.stream().
                    map(ColorCovidManifestParser.Dto::getLabel).
                    collect(Collectors.toList());
            String notInRack = CollectionUtils.subtract(manifestTubeBarcodes, rackScanBarcodes).stream().
                    sorted().collect(Collectors.joining(" "));
            if (!notInRack.isEmpty()) {
                messages.addError("These manifest tubes are not in the rack: " + notInRack);
            }
            String notInManifest = CollectionUtils.subtract(rackScanBarcodes, manifestTubeBarcodes).stream().
                    sorted().collect(Collectors.joining(" "));
            if (!notInManifest.isEmpty()) {
                messages.addError("These rack tubes are not in the manifest: " + notInManifest);
            }
            if (!messages.hasErrors()) {
                // Copies the rack scan well positions into the dtos, since the manifest did not provide them.
                Map<String, String> reverseRackScan = bean.getRackScan().entrySet().stream().
                        filter(mapEntry -> StringUtils.isNotBlank(mapEntry.getValue())).
                        collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
                dtos.forEach(dto -> dto.getSampleMetadata().
                        put(Metadata.Key.WELL_POSITION, reverseRackScan.get(dto.getLabel())));
            }
        }
        if (!messages.hasErrors()) {
            messages.addInfo("Manifest and tubes agree.");
        }

        // Checks for existing MercurySamples.
        String existingSamples = mercurySampleDao.findMapIdToMercurySample(vesselLabels).entrySet().stream().
                filter(mapEntry -> mapEntry.getValue() != null).
                map(Map.Entry::getKey).
                sorted().collect(Collectors.joining(", "));
        if (!existingSamples.isEmpty()) {
            messages.addError("Mercury already has sample(s) named " + existingSamples);
        }

        if (!messages.hasErrors()) {
            // Writes the dtos and the original manifest to the bean for use in accessioning phase.
            bean.setDtoString(colorCovidManifestParser.getDtoString());
            bean.setManifestContent(new String(content));
            messages.addInfo(dtos.size() + " samples can be accessioned.");
        }
    }

    public void accession(ColorCovidReceiptActionBean bean) {
        Date now = new Date();
        MessageCollection messages = bean.getMessageCollection();
        final String rackBarcode = bean.getRackBarcode();
        // Reconstructs the dtos.
        List<ColorCovidManifestParser.Dto> dtos = new ColorCovidManifestParser(null, null).
                parseDtos(bean.getDtoString(), messages);
        if (!messages.hasErrors()) {
            // Writes the manifest file to the Covid manifests bucket.
            covidManifestCopier.copyContentToBucket(FilenameUtils.getName(bean.getFilename()),
                    bean.getManifestContent().getBytes());

            List<Object> newEntities = new ArrayList<>();
            Map<VesselPosition, BarcodedTube> vesselPositionToTube = new HashMap<>();
            for (ColorCovidManifestParser.Dto dto : dtos) {
                // Creates the tube.
                BarcodedTube tube = new BarcodedTube(dto.getLabel(), DEFAULT_TUBE_TYPE);
                newEntities.add(tube);
                // Creates the sample.
                Set<Metadata> sampleMetadata = dto.getSampleMetadata().entrySet().stream().
                        map(mapEntry -> new Metadata(mapEntry.getKey(), mapEntry.getValue())).
                        collect(Collectors.toSet());
                sampleMetadata.add(Metadata.createMetadata(Metadata.Key.RECEIPT_DATE, now));
                MercurySample sample = new MercurySample(dto.getSampleName(), MercurySample.MetadataSource.MERCURY);
                sample.addMetadata(sampleMetadata);
                newEntities.add(sample);
                // Links tube, sample, position.
                tube.addSample(sample);
                String position = dto.getSampleMetadata().get(Metadata.Key.WELL_POSITION);
                vesselPositionToTube.put(VesselPosition.getByName(position), tube);
            }
            // Creates the rack and tube formation.
            List<Pair<VesselPosition, String>> tubeFormationPairs = vesselPositionToTube.entrySet().stream().
                    map(mapEntry -> Pair.of(mapEntry.getKey(), mapEntry.getValue().getLabel())).
                    collect(Collectors.toList());
            TubeFormation tubeFormation = tubeFormationDao.findByDigest(TubeFormation.makeDigest(tubeFormationPairs));
            if (tubeFormation == null) {
                tubeFormation = new TubeFormation(vesselPositionToTube, DEFAULT_RACK_TYPE);
                newEntities.add(tubeFormation);
            }
            RackOfTubes rack = new RackOfTubes(bean.getRackBarcode(), DEFAULT_RACK_TYPE);
            newEntities.add(rack);
            tubeFormation.addRackOfTubes(rack);
            rack.getTubeFormations().add(tubeFormation);

            labVesselDao.persistAll(newEntities);
            messages.addInfo("Accessioned rack " + rackBarcode + " having " + dtos.size() + " sample tubes " +
                    " (for Transfer Visualizer use barcode " + tubeFormation.getDigest() + " ).");
            // Resets the dto and manifest info for the jsp.
            bean.setDtoString(null);
            bean.setManifestContent(null);
        }
    }
}
