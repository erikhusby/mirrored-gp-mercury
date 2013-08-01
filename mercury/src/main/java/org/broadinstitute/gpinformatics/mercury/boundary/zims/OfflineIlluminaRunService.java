package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import edu.mit.broad.prodinfo.thrift.lims.IndexPosition;
import edu.mit.broad.prodinfo.thrift.lims.MolecularIndexingScheme;
import edu.mit.broad.prodinfo.thrift.lims.TZDevExperimentData;
import edu.mit.broad.prodinfo.thrift.lims.TZReadType;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRead;
import org.broadinstitute.gpinformatics.infrastructure.Offline;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaChamber;
import org.broadinstitute.gpinformatics.mercury.entity.zims.ZimsIlluminaRun;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
        return makeRun(runName, 8, 12);
    }

    public static ZimsIlluminaRun makeRun(String runName, int numLanes, int numLibraries) {
        ZimsIlluminaRun run = new ZimsIlluminaRun(runName, "Run-123", "Flowcell-123", "Sequencer 123", "Test Sequencer", "05/11/2012 17:08", false, "36T8B", 999.987654,null );
        run.addRead(new TZamboniRead((short) 1, (short) 10, TZReadType.INDEX));
        run.addRead(new TZamboniRead((short) 11, (short) 20, TZReadType.TEMPLATE));
        for (int i = 1; i <= numLanes; i++) {
            run.addLane(makeLane(i, numLibraries));
        }
        return run;
    }

    public static ZimsIlluminaChamber makeLane(int laneNumber, int numLibraries) {
        List<LibraryBean> libraries = new ArrayList<>();
        libraries.toArray(new LibraryBean[] {});
        for (int i = 0; i < numLibraries; i++) {
            libraries.add(makeLibrary(Integer.toString(libraryNumber)));
            libraryNumber++;
        }
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2013, Calendar.JUNE, 25, 14, 15);
        return new ZimsIlluminaChamber((short) laneNumber, libraries, "PESP1+T", "LaneLibrary-"+laneNumber, cal.getTime());
    }

    private ZimsIlluminaChamber makeLane(int laneNumber) {
        return makeLane(laneNumber, 12);
    }

    private static LibraryBean makeLibrary(String number) {
        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            conditions.add("condition " + number + "-" + i);
        }
        List<String> gssrBarcodes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            gssrBarcodes.add(number + "." + i);
        }
        TZDevExperimentData experimentData = new TZDevExperimentData("Experiment " + number, conditions);
        return new LibraryBean("Library-" + number, "Project-" + number, "Initiative-" + number, 1L, new MolecularIndexingScheme("IndexingScheme-" + number, new HashMap<IndexPosition, String>()), Boolean.TRUE, number, "Analysis-" + number, "Reference-" + number, "RefVer-" + number, "Sample-" + number, "Organism-" + number, "Species-" + number, "Strain-" + number, "LSID-" + number, "Aligner-" + number, "Size Range " + number, "Enzyme-" + number, "Bait-" + number, "Individual-" + number, 123.4, Boolean.FALSE, Boolean.FALSE, experimentData, gssrBarcodes, "GSSR SampleType-" + number, Boolean.FALSE, new ArrayList<String>(), null, null, null);
    }
}
