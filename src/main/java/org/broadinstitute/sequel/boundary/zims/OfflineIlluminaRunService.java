package org.broadinstitute.sequel.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import org.broadinstitute.sequel.entity.zims.LibraryBean;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.infrastructure.Offline;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author breilly
 */
@Offline
public class OfflineIlluminaRunService implements IlluminaRunService, Serializable {
    @Override
    public ZimsIlluminaRun getRun(String runName) {
        ZimsIlluminaRun run = new ZimsIlluminaRun(runName, null, null, null, null, "05/11/2012 17:08", (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, false);
        List<LibraryBean> libraries = new ArrayList<LibraryBean>();
        libraries.add(new LibraryBean("Library-123", "Project-123", "Initiative-123", 1L, new MolecularIndexingScheme("IndexingScheme-123", new HashMap<IndexPosition, String>()), Boolean.TRUE, "234", "Analysis-123", "Reference-123", "RefVer-123", "Sample-123", "Collaborator-123", "Organism-123", "Species-123", "Strain-123", "LSID-123", "Tissue-123", "Plasmid-123", "Aligner-123", "Size Range 1-3", "Enzyme-123", "CellLine-123", "Bait-123", "Individual-123", 234.5, Boolean.FALSE, Boolean.FALSE, "no weirdness", 123.4, Boolean.FALSE, new TZDevExperimentData(null, null), "GSSR-123", null, "GSSR SampleType-123", Short.valueOf((short) 1), Boolean.FALSE));
        run.addLane(new ZimsIlluminaChamber((short) 1, libraries, "PESP1+T"));
        run.addLane(new ZimsIlluminaChamber((short) 2, libraries, "PESP1+T"));
        run.addLane(new ZimsIlluminaChamber((short) 3, libraries, "PESP1+T"));
        return run;
    }
}
