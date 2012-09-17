package org.broadinstitute.sequel.control.vessel;

import java.io.InputStream;
import java.util.List;

/**
 * Parse an input file containing indexed plate definitions
 */
public interface IndexedPlateParser {
    List<PlateWellIndexAssociation> parseInputStream(InputStream is);
}
