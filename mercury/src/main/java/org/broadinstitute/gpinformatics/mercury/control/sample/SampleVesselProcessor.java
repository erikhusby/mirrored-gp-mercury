package org.broadinstitute.gpinformatics.mercury.control.sample;

import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ParentVesselBean;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

/**
 * Methods common to sample vessel spreadsheet parsers.
 */
public abstract class SampleVesselProcessor extends TableProcessor {

    protected SampleVesselProcessor(String sheetName, @Nonnull IgnoreTrailingBlankLines ignoreTrailingBlankLines) {
        super(sheetName, ignoreTrailingBlankLines);
    }

    public abstract Set<String> getTubeBarcodes();

    public abstract Set<String> getSampleIds();

    public abstract List<ParentVesselBean> getParentVesselBeans();
}
