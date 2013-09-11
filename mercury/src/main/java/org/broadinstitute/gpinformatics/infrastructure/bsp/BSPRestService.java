package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleDetails;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Service for making RESTful calls into BSP, this lives separately from the tab-delimited services as the real
 * implementation must set the response MIME type to MediaType.APPLICATION_XML_TYPE.
 */
public interface BSPRestService extends Serializable {
    Map<String,SampleInfo> fetchSampleDetailsByMatrixBarcodes(@Nonnull Collection<String> matrixBarcodes);
}
