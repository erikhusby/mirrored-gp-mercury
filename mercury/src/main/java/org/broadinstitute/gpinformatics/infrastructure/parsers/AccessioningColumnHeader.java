package org.broadinstitute.gpinformatics.infrastructure.parsers;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.util.ArrayList;
import java.util.List;

public interface AccessioningColumnHeader extends ColumnHeader {
    String NO_MANIFEST_HEADER_FOUND_FOR_COLUMN =
            "No ManifestHeader found for columnHeader: ";

    public Metadata.Key getMetadataKey();
    public String getColumnName();
}
