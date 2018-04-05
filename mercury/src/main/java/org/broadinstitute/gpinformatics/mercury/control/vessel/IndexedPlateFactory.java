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
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IndexPosition.FOUR54_A.getTechnology()),
                false),
        ION_SINGLE("Ion Torrent (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IndexPosition.ION_A.getTechnology()),
                false),
        ILLUMINA_SINGLE("Illumina (Single Index)",
                new IndexedPlateParserIDTSpreadsheetFormat(),
                true),
        ILLUMINA_TSCA("Illumina (TSCA)",
                new IndexedPlateParserTSCAFormat(),
                false),
        ILLUMINA_FP("Illumina (FP)",
                new IndexedPlateParserTSCAFormat(), StaticPlate.PlateType.IndexedAdapterPlate384,
                true);

        private final String prettyName;
        private final IndexedPlateParser indexedPlateParser;
        private final StaticPlate.PlateType plateType;
        private final boolean active;

        TechnologiesAndParsers(String name, IndexedPlateParser indexedPlateParser, boolean active) {
            prettyName = name;
            this.indexedPlateParser = indexedPlateParser;
            this.plateType = StaticPlate.PlateType.IndexedAdapterPlate96;
            this.active = active;
        }

        TechnologiesAndParsers(String prettyName,
                               IndexedPlateParser indexedPlateParser,
                               StaticPlate.PlateType plateType, boolean active) {
            this.prettyName = prettyName;
            this.indexedPlateParser = indexedPlateParser;
            this.plateType = plateType;
            this.active = active;
        }

        public String getPrettyName() {
            return prettyName;
        }

        public IndexedPlateParser getIndexedPlateParser() {
            return indexedPlateParser;
        }

        public StaticPlate.PlateType getPlateType() {
            return plateType;
        }

        public boolean isActive() {
            return active;
        }
    }

    public Map<String, StaticPlate> parseAndPersist(TechnologiesAndParsers technologiesAndParsers,
            InputStream inputStream) {
        Map<String, StaticPlate> platesByBarcode = parseStream(inputStream, technologiesAndParsers);
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
        IndexedPlateParser parser = technologiesAndParsers.getIndexedPlateParser();
        List<PlateWellIndexAssociation> associations;
        try {
            associations = parser.parseInputStream(inputStream);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
        return uploadIndexedPlates(associations, technologiesAndParsers.getPlateType());
    }

    public Map<String, StaticPlate> uploadIndexedPlates(List<PlateWellIndexAssociation> plateWellIndexes,
                                                        StaticPlate.PlateType plateType) {
        Map<String, StaticPlate> platesByBarcode = new HashMap<>();
        Set<PlateWell> previousWells = new HashSet<>();

        for (PlateWellIndexAssociation plateWellIndex : plateWellIndexes) {
            StaticPlate plate = createOrGetPlate(
                    plateWellIndex,
                    platesByBarcode,
                    plateType);
            VesselPosition vesselPosition = VesselPosition.getByName(plateWellIndex.getWellName());
            PlateWell plateWell = new PlateWell(plate, vesselPosition);
            if (previousWells.contains(plateWell)) {
                throw new RuntimeException(
                        "Plate " + plate.getLabel() + " and well " + plateWellIndex.getWellName() +
                        " has been defined two or more times in the uploaded file.");
            }
            previousWells.add(plateWell);

            MolecularIndexingScheme indexingScheme = indexingSchemeFactory.findOrCreateIndexingScheme(
                    Arrays.asList(plateWellIndex.getPositionPairs()), false);
            plateWell.addReagent(new MolecularIndexReagent(indexingScheme));

            plate.getContainerRole().addContainedVessel(plateWell, vesselPosition);
        }

        LOG.info("Number of plates: " + platesByBarcode.keySet().size());
        return platesByBarcode;
    }


    private StaticPlate createOrGetPlate(PlateWellIndexAssociation plateWellIndex,
                                         Map<String, StaticPlate> platesByBarcode, StaticPlate.PlateType plateType) {

        String formattedBarcode = StringUtils.leftPad(plateWellIndex.getPlateBarcode(), BARCODE_LENGTH, '0');
        StaticPlate plate = platesByBarcode.get(formattedBarcode);
        if (plate == null) {
            plate = new StaticPlate(formattedBarcode, plateType);
            plate.setCreatedOn(new Date());
            platesByBarcode.put(formattedBarcode, plate);
        }
        return plate;
    }
}
