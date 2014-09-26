package org.broadinstitute.gpinformatics.mercury.control.zims;

import edu.mit.broad.prodinfo.thrift.lims.TZamboniLibrary;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;

/**
 * Given a {@link TZamboniLibrary}, convert it into a mercury {@link LibraryBean}.
 * Useful for testing.
 */
public interface ThriftLibraryConverter {

    public LibraryBean convertLibrary(TZamboniLibrary zamboniLibrary, SampleData sampleData, ProductOrder pdo);
}
