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

    public SquidThriftLibraryConverter() {}

    @Override
    public LibraryBean convertLibrary(TZamboniLibrary zamboniLibrary,BSPSampleDTO bspDTO,ProductOrder pdo) {
        String organism = null;

        // todo arz extract more fields from bsp here.

        // todo arz test with all bsp data, some bsp samples and some gssr samples
        // test with null PDO and real PDOs
        // test flowcell query method
        // benchmark performance

        // todo arz figure out integration test w/ bsp service, get rid of EverythingYouAskForAndItsHuman mock

        LibraryBean libBean = new LibraryBean(zamboniLibrary.getLibrary(),
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
                zamboniLibrary.getSampleCollaborator(),
                zamboniLibrary.getOrganism(),
                zamboniLibrary.getSpecies(),
                zamboniLibrary.getStrain(),
                zamboniLibrary.getLsid(),
                zamboniLibrary.getTissueType(),
                zamboniLibrary.getExpectedPlasmid(),
                zamboniLibrary.getAligner(),
                zamboniLibrary.getRrbsSizeRange(),
                zamboniLibrary.getRestrictionEnzyme(),
                zamboniLibrary.getCellLine(),
                zamboniLibrary.getBaitSetName(),
                zamboniLibrary.getIndividual(),
                zamboniLibrary.getLabMeasuredInsertSize(),
                zamboniLibrary.isPositiveControl(),
                zamboniLibrary.isNegativeControl(),
                zamboniLibrary.getWeirdness(),
                zamboniLibrary.getPrecircularizationDnaSize(),
                zamboniLibrary.isPartOfDevExperiment(),
                zamboniLibrary.getDevExperimentData(),
                zamboniLibrary.getGssrBarcode(),
                zamboniLibrary.getGssrBarcodes(),
                zamboniLibrary.getGssrSampleType(),
                zamboniLibrary.getTargetLaneCoverage(),
                zamboniLibrary.aggregate,
                zamboniLibrary.getCustomAmpliconSetNames(),
                zamboniLibrary.isFastTrack(),
                pdo,
                zamboniLibrary.getLcset(),
                bspDTO);
        return libBean;
    }
}
