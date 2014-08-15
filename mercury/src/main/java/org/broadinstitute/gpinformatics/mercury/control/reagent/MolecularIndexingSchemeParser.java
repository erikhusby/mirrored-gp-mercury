package org.broadinstitute.gpinformatics.mercury.control.reagent;

import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses a text file to create {@link MolecularIndexingScheme}s.
 * The text file contains headers that match the {@link MolecularIndexingScheme.IndexPosition} enum values.
 * An optional NAME header can be used to specify grand-fathered names that pre-date the introduction of
 * automatic names, e.g. tagged_960.
 */
public class MolecularIndexingSchemeParser {
    @Inject
    private MolecularIndexingSchemeFactory molecularIndexingSchemeFactory;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final String NAME_HEADER = "NAME";

    public List<MolecularIndexingScheme> parse(InputStream inputStream) {
        List<MolecularIndexingScheme> molecularIndexingSchemes = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            // Get optional name, and index positions from header.
            String headerLine = bufferedReader.readLine();
            if (headerLine == null) {
                throw new RuntimeException("No header line");
            }
            String[] headers = WHITESPACE.split(headerLine.trim());
            List<MolecularIndexingScheme.IndexPosition> indexPositions = new ArrayList<>();
            int headerIndex = 0;
            boolean hasNameHeader = false;
            for (String header : headers) {
                if (header.equals(NAME_HEADER)) {
                    if (headerIndex > 0) {
                        throw new RuntimeException(NAME_HEADER + " must be first");
                    }
                    hasNameHeader = true;
                } else {
                    MolecularIndexingScheme.IndexPosition indexPosition =
                            MolecularIndexingScheme.IndexPosition.valueOf(header);
                    if (indexPosition == null) {
                        throw new RuntimeException("Failed to find IndexPosition " + header);
                    }
                    indexPositions.add(indexPosition);
                }
                headerIndex++;
            }

            // Create schemes.
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] fields = WHITESPACE.split(line.trim());
                if (fields.length != headerIndex) {
                    throw new RuntimeException("Expected " + headerIndex + " fields, found " + fields.length +
                                               " : " + line);
                }
                if (hasNameHeader) {
                    // Use provided name, only for grand-fathered names.
                    String schemeName = fields[0];
                    // Find existing scheme, if any.
                    MolecularIndexingScheme molecularIndexingScheme = molecularIndexingSchemeDao.findByName(schemeName);
                    if (molecularIndexingScheme == null) {
                        molecularIndexingScheme = new MolecularIndexingScheme();
                        molecularIndexingScheme.setName(schemeName);
                        for (int i = 1; i < fields.length; i++) {
                            String sequence = fields[i];
                            MolecularIndex molecularIndex = molecularIndexDao.findBySequence(sequence);
                            if (molecularIndex == null) {
                                molecularIndex = new MolecularIndex(sequence);
                            }
                            molecularIndexingScheme.addIndexPosition(indexPositions.get(i - 1), molecularIndex);
                        }
                        molecularIndexingSchemeDao.persist(molecularIndexingScheme);
                    }
                    molecularIndexingSchemes.add(molecularIndexingScheme);
                } else {
                    // Auto-generate name.
                    List<MolecularIndexingSchemeFactory.IndexPositionPair> indexPositionPairs = new ArrayList<>();
                    for (int i = 0; i < fields.length; i++) {
                        indexPositionPairs.add(new MolecularIndexingSchemeFactory.IndexPositionPair(
                                indexPositions.get(i), fields[i]));
                    }
                    molecularIndexingSchemes.add(
                            molecularIndexingSchemeFactory.findOrCreateIndexingScheme(indexPositionPairs));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException ignored) {
            }
        }
        return molecularIndexingSchemes;
    }
}
