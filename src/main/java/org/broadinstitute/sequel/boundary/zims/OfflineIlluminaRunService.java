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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author breilly
 */
@Offline
public class OfflineIlluminaRunService implements IlluminaRunService, Serializable {

    private static int libraryNumber = 100;

    @Override
    public ZimsIlluminaRun getRun(String runName) {
        ZimsIlluminaRun run = new ZimsIlluminaRun(runName, null, null, null, null, "05/11/2012 17:08", (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, false);
        for (int i = 1; i <= 8; i++) {
            run.addChamber(makeLane(i));
        }
        return run;
    }

    private ZimsIlluminaChamber makeLane(int laneNumber) {
        List<LibraryBean> libraries = new ArrayList<LibraryBean>();
        libraries.toArray(new LibraryBean[] {});
        for (int i = 0; i < 12; i++) {
            libraries.add(makeLibrary(Integer.toString(libraryNumber)));
            libraryNumber++;
        }
        return new ZimsIlluminaChamber((short) laneNumber, libraries, "PESP1+T");
    }

    private LibraryBean makeLibrary(String number) {
        List<String> conditions = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            conditions.add("condition " + number + "-" + i);
        }
        List<String> gssrBarcodes = new ArrayList<String>();
        for (int i = 0; i < 3; i++) {
            gssrBarcodes.add(number + "." + i);
        }
        TZDevExperimentData experimentData = new TZDevExperimentData("Experiment " + number, conditions);
        return new LibraryBean("Library-" + number, "Project-" + number, "Initiative-" + number, 1L, new MolecularIndexingScheme("IndexingScheme-" + number, new HashMap<IndexPosition, String>()), Boolean.TRUE, number, "Analysis-" + number, "Reference-" + number, "RefVer-" + number, "Sample-" + number, "Collaborator-" + number, "Organism-" + number, "Species-" + number, "Strain-" + number, "LSID-" + number, "Tissue-" + number, "Plasmid-" + number, "Aligner-" + number, "Size Range " + number, "Enzyme-" + number, "CellLine-" + number, "Bait-" + number, "Individual-" + number, 123.4, Boolean.FALSE, Boolean.FALSE, "no weirdness", 123.4, Boolean.FALSE, experimentData, "GSSR-" + number, gssrBarcodes, "GSSR SampleType-" + number, Short.valueOf((short) 1), Boolean.FALSE);
    }
}
