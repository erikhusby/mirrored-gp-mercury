package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;

/**
 * Standard way to get from a {@link TZamboniLibrary} to a {@link LibraryBean}
 * when starting from squid's thrift service
 */
public class SquidThriftLibraryConverter implements ThriftLibraryConverter {

    public SquidThriftLibraryConverter() {
    }

    @Override
    public LibraryBean convertLibrary(TZamboniLibrary zamboniLibrary, BSPSampleDTO bspDTO, ProductOrder pdo) {
        // todo arz extract more fields from bsp here.

        // todo arz test with all bsp data, some bsp samples and some gssr samples
        // test with null PDO and real PDOs
        // test flowcell query method
        // benchmark performance

        // todo arz figure out integration test w/ bsp service, get rid of EverythingYouAskForAndItsHuman mock

        return new LibraryBean(zamboniLibrary.getLibrary(),
                zamboniLibrary.getProject(),
                zamboniLibrary.getInitiative(),
                zamboniLibrary.getWorkRequestId(),
                zamboniLibrary.getMolecularIndexes(),
                zamboniLibrary.isHasIndexingRead(),
                zamboniLibrary.getExpectedInsertSize(),
                zamboniLibrary.getAnalysisType(),
                zamboniLibrary.getReferenceSequence(),
                zamboniLibrary.getReferenceSequenceVersion(),
                zamboniLibrary.getSampleAlias(),
                zamboniLibrary.getOrganism(),
                zamboniLibrary.getSpecies(),
                zamboniLibrary.getStrain(),
                zamboniLibrary.getLsid(),
                zamboniLibrary.getAligner(),
                zamboniLibrary.getRrbsSizeRange(),
                zamboniLibrary.getRestrictionEnzyme(),
                zamboniLibrary.getBaitSetName(),
                zamboniLibrary.getIndividual(),
                zamboniLibrary.getLabMeasuredInsertSize(),
                zamboniLibrary.isPositiveControl(),
                zamboniLibrary.isNegativeControl(),
                zamboniLibrary.getDevExperimentData(),
                zamboniLibrary.getGssrBarcodes(),
                zamboniLibrary.getGssrSampleType(),
                zamboniLibrary.aggregate,
                zamboniLibrary.getCustomAmpliconSetNames(),
                pdo,
                zamboniLibrary.getLcset(),
                bspDTO,
                zamboniLibrary.getLabWorkflow(),
                zamboniLibrary.getPdoSample());
    }
}
