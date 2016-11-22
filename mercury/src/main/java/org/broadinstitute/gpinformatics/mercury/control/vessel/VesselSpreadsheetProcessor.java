package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class VesselSpreadsheetProcessor extends TableProcessor {

    protected VesselSpreadsheetProcessor (String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        super(sheetName, ignoreTrailingBlankLines);
    }

    public abstract List<String> getSingleSampleLibraryName();

    public abstract List<String> getBroadSampleId();

    public abstract List<String> getRootSampleId();

    public abstract List<String> getMolecularIndexingScheme();

    public abstract List<String> getBait();

    public abstract List<String> getCat();

    public abstract  List<String> getBarcodes();

    public abstract List<String>  getExperiment();

    public abstract List<Map<String, String>> getConditions();

    public abstract List<String> getCollaboratorSampleId();

    public abstract List<String> getBroadParticipantId();

    public abstract List<String> getCollaboratorParticipantId();

    public abstract List<String> getGender();

    public abstract List<String> getSpecies();

    public abstract List<String> getLsid();


}
