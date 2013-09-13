package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class creates plates containing molecular index reagents
 */
@Stateless
public class IndexedPlateFactory {
    private static final Log LOG = LogFactory.getLog(IndexedPlateFactory.class);
    public static final int BARCODE_LENGTH = 12;

    @Inject
    private MolecularIndexingSchemeFactory indexingSchemeFactory;

    @Inject
    private StaticPlateDao staticPlateDao;

    public enum TechnologiesAndParsers {
        FOUR54_SINGLE("454 (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IndexPosition.FOUR54_A.getTechnology())),
        ION_SINGLE("Ion Torrent (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IndexPosition.ION_A.getTechnology())),
        ILLUMINA_SINGLE("Illumina (Single Index)",
                new IndexedPlateParserIDTSpreadsheetFormat()),
        ILLUMINA_TSCA("Illumina (TSCA)",
                new IndexedPlateParserTSCAFormat());

        private final String prettyName;
        private final IndexedPlateParser indexedPlateParser;

        TechnologiesAndParsers(final String name, IndexedPlateParser indexedPlateParser) {
            this.prettyName = name;
            this.indexedPlateParser = indexedPlateParser;
        }

        public String getPrettyName() {
            return this.prettyName;
        }

        public IndexedPlateParser getIndexedPlateParser() {
            return indexedPlateParser;
        }
    }

    public Map<String, StaticPlate> parseAndPersist(File file, TechnologiesAndParsers technologiesAndParsers) {
        Map<String, StaticPlate> platesByBarcode = null;
        try {
            platesByBarcode = parseStream(new FileInputStream(file), technologiesAndParsers);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        for (StaticPlate staticPlate : platesByBarcode.values()) {
            if (staticPlateDao.findByBarcode(staticPlate.getLabel()) != null) {
                throw new RuntimeException("Plate already exists: " + staticPlate.getLabel());
            }
            staticPlateDao.persist(staticPlate);
            staticPlateDao.flush();
            staticPlateDao.clear();
        }
        return platesByBarcode;
    }

    public Map<String, StaticPlate> parseStream(InputStream inputStream,
                                                TechnologiesAndParsers technologiesAndParsers) {
        final IndexedPlateParser parser = technologiesAndParsers.getIndexedPlateParser();
        final List<PlateWellIndexAssociation> associations;
        try {
            associations = parser.parseInputStream(inputStream);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
        Map<String, StaticPlate> platesByBarcode = uploadIndexedPlates(associations);
//                setSuccessText("Uploaded " + associations.size() + " rows from " + file.getName());

        return platesByBarcode;
    }

    public Map<String, StaticPlate> uploadIndexedPlates(final List<PlateWellIndexAssociation> plateWellIndexes
/*, final String technology*/) {
        final Map<String, StaticPlate> platesByBarcode = new HashMap<>();
        final Set<PlateWell> previousWells = new HashSet<>();


        for (final PlateWellIndexAssociation plateWellIndex : plateWellIndexes) {
            final StaticPlate plate = this.createOrGetPlate(
                    plateWellIndex,
                    platesByBarcode);
            VesselPosition vesselPosition = VesselPosition.getByName(plateWellIndex.getWellName());
            final PlateWell plateWell = new PlateWell(plate, vesselPosition);
            if (previousWells.contains(plateWell)) {
                throw new RuntimeException(
                        "Plate " + plate.getLabel() + " and well " + plateWellIndex.getWellName() +
                        " has been defined two or more times in the uploaded file.");
            }
            previousWells.add(plateWell);

            final MolecularIndexingScheme indexingScheme =
                    this.indexingSchemeFactory
                            .findOrCreateIndexingScheme(Arrays.asList(plateWellIndex.getPositionPairs()));
            plateWell.addReagent(new MolecularIndexReagent(indexingScheme));

            plate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
        }

        LOG.info("Number of plates: " + platesByBarcode.keySet().size());
        return platesByBarcode;
    }


    private StaticPlate createOrGetPlate(final PlateWellIndexAssociation plateWellIndex,
                                         final Map<String, StaticPlate> platesByBarcode) {
        final String formattedBarcode = StringUtils.leftPad(plateWellIndex.getPlateBarcode(), BARCODE_LENGTH, '0');
        StaticPlate plate = platesByBarcode.get(formattedBarcode);
        if (plate == null) {
            plate = new StaticPlate(formattedBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
            plate.setCreatedOn(new Date());
            platesByBarcode.put(formattedBarcode, plate);
        }
        return plate;
    }
}
