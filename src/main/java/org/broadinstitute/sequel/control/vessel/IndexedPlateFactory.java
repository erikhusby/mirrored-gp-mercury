package org.broadinstitute.sequel.control.vessel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.sequel.control.reagent.MolecularIndexingSchemeFactory;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.sequel.entity.vessel.PlateWell;
import org.broadinstitute.sequel.entity.vessel.StaticPlate;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
public class IndexedPlateFactory {
    private static final Log gLog = LogFactory.getLog(IndexedPlateFactory.class);

    @Inject
    private MolecularIndexingSchemeFactory indexingSchemeFactory;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    public enum TechnologiesAndParsers {
        FOUR54_SINGLE("454 (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.Four54PositionHint.A.getTechnology())),
        ION_SINGLE("Ion Torrent (Single Index)",
                new IndexedPlateParserFourColumnFormat(MolecularIndexingScheme.IonPositionHint.A.getTechnology())),
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
    }

    public Map<String, StaticPlate> parseFile(File file, TechnologiesAndParsers technologiesAndParsers) {
        Map<String, StaticPlate> platesByBarcode = null;
        if (file == null) {
            throw new RuntimeException("Please enter a file name.");
        } else {
            final IndexedPlateParser parser = technologiesAndParsers.indexedPlateParser;
            final List<PlateWellIndexAssociation> associations;
            try {
                associations = parser.parseInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            platesByBarcode = uploadIndexedPlates(associations);
//                setSuccessText("Uploaded " + associations.size() + " rows from " + file.getName());
        }

        return platesByBarcode;
    }

    public Map<String, StaticPlate> uploadIndexedPlates(final List<PlateWellIndexAssociation> plateWellIndexes/*, final String technology*/) {
        final Map<String, StaticPlate> platesByBarcode = new HashMap<String, StaticPlate>();
        final Set<PlateWell> previousWells = new HashSet<PlateWell>();


        for (final PlateWellIndexAssociation plateWellIndex : plateWellIndexes) {
            final StaticPlate plate = this.createOrGetPlate(
                    plateWellIndex,
                    platesByBarcode);
            final PlateWell plateWell = new PlateWell(plate, VesselPosition.valueOf(plateWellIndex.getWellName()));
            if (previousWells.contains(plateWell)) {
                throw new RuntimeException(
                        "Plate " + plate.getLabel() + " and well " + plateWellIndex.getWellName() +
                                " has been defined two or more times in the uploaded file.");
            }
            previousWells.add(plateWell);

            final MolecularIndexingScheme indexingScheme =
                    this.indexingSchemeFactory.findOrCreateIndexingScheme(Arrays.asList(plateWellIndex.getPositionPairs()));
            plateWell.addReagent(new MolecularIndexReagent(indexingScheme));

            plate.getVesselContainer().addContainedVessel(plateWell, plateWellIndex.getWellName());
//            this.em.persist(new PlateSeqSampleIdentifier(plate, wellDescription, seqSampleIdentifier, indexingScheme));
        }

        gLog.info("Number of plates: " + platesByBarcode.keySet().size());
//        this.em.flush();
        return platesByBarcode;
    }


    /*
      * Adds leading zeros to make a 12-digit barcode.
      */
    private String getFormattedBarcode(final String rawBarcode) {
        String formattedBarcode = rawBarcode;
        for (int j = rawBarcode.length(); j < 12; j++) {
            formattedBarcode = "0" + formattedBarcode;
        }
        return formattedBarcode;
    }

    private StaticPlate createOrGetPlate(final PlateWellIndexAssociation plateWellIndex, final Map<String, StaticPlate> platesByBarcode) {
        final String formattedBarcode = this.getFormattedBarcode(plateWellIndex.getPlateBarcode());
        StaticPlate plate = platesByBarcode.get(formattedBarcode);
        if (plate == null) {
/*
todo jmt restore this check elsewhere
            if (staticPlateDAO.findByBarcode(formattedBarcode) != null) {
                throw new RuntimeException("Plate already exists: " + formattedBarcode);
            }
*/

            plate = new StaticPlate(formattedBarcode, StaticPlate.PlateType.IndexedAdapterPlate96);
            plate.setCreatedOn(new Date());
            platesByBarcode.put(formattedBarcode, plate);

//            staticPlateDAO.persist(plate);
        }
        return plate;
    }
}
