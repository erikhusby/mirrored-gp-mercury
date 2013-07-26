package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of indexed-plate upload input parser for 454 plates
 * Tab-delimited Text file with 4 columns: plate barcode, well name, index name , index sequence
 * First line is a header and will be ignored
 */
public class IndexedPlateParserFourColumnFormat implements IndexedPlateParser {

    private final String technology;

    public IndexedPlateParserFourColumnFormat(final String tech) {
        this.technology = tech;
    }

    @Override
    public List<PlateWellIndexAssociation> parseInputStream(final InputStream is) {
        final List<PlateWellIndexAssociation> plateIndexes = new ArrayList<>();

        try {
            final StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer);

            final String[] records = writer.toString().trim().split("\r\n|\r|\n");

            //ignore header row
            for (int i = 1; i < records.length; i++) {
                final String[] fields = records[i].trim().split("\t");
                // skip over short rows, e.g. the well is empty
                if (fields.length == 4) {
                    final String plateBarcode = fields[0];
                    final String wellName = fields[1];
                    final String sequence = fields[3];

                    final PlateWellIndexAssociation association = new PlateWellIndexAssociation(
                            plateBarcode,
                            wellName,
                            this.technology);
                    association.addIndex(MolecularIndexingScheme.getDefaultPositionHint(this.technology), sequence);
                    plateIndexes.add(association);
                }
            }
        } catch (final IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return plateIndexes;
    }
}
