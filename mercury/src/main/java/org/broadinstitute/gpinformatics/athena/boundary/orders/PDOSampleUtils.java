package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDOSampleUtils {

    /**
     * Lines up the PDOSamples passed in
     * from the client with the query results
     * that came from the database.
     * @param requestedPdoSamples the pdo sample pairs
     *                                as it came in from the client.
     *                                @see org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderResource#getPdoSampleBillingStatus(PDOSamples)
     * @param pdoSamples the pdo samples that were returned
     *                   from the database.
     *                   @see org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao#findByOrderKeyAndSampleNames(String, java.util.Set)
     * @return a new PDOSamples object that has the right billing
     * status and has error fields set for pdo/sample pairs
     * that were not found.
     */
    @DaoFree
    public static PDOSamples buildOutputPDOSamplePairsFromInputAndQueryResults(PDOSamples requestedPdoSamples, List<ProductOrderSample> pdoSamples) {
        PDOSamples pdoSamplesResults = new PDOSamples();
        for (PDOSample requestedPdoSample : requestedPdoSamples.getPdoSamples()) {
            boolean foundIt = false;
            String requestedPdoKey = requestedPdoSample.getPdoKey();
            String requestedSampleName = requestedPdoSample.getSampleName();
            for (ProductOrderSample pdoSample : pdoSamples) {
                if (requestedPdoKey.equals(pdoSample.getProductOrder().getBusinessKey()) && requestedSampleName.equals(pdoSample.getName())) {
                    pdoSamplesResults.addPdoSamplePair(requestedPdoKey,requestedSampleName,pdoSample.isCompletelyBilled());
                    foundIt = true;
                }
            }
            if (!foundIt) {
                pdoSamplesResults.addPdoSamplePair(requestedPdoKey,requestedSampleName,null);
                String errorMessage = MessageFormat.format("Could not find sample {0} in PDO {1}.",requestedSampleName,requestedPdoKey);
                pdoSamplesResults.addError(errorMessage);
            }
        }
        return pdoSamplesResults;
    }

    /**
     * Converts the list of pdo/sample tuples to a map
     * where the key is the PDO key and the value is
     * a list of sample names for that PDO.
     * @param pdoSamples
     * @return
     */
    @DaoFree
    public static Map<String,Set<String>> convertPdoSamplePairsListToMap(@Nonnull PDOSamples pdoSamples) {
        Map<String,Set<String>> pdoToSamples = new HashMap<>();
        for (PDOSample pdoSample : pdoSamples.getPdoSamples()) {
            String pdoKey = pdoSample.getPdoKey();
            if (!pdoToSamples.containsKey(pdoKey)) {
                pdoToSamples.put(pdoSample.getPdoKey(),new HashSet<String>());
            }
            pdoToSamples.get(pdoKey).add(pdoSample.getSampleName());
        }
        return pdoToSamples;
    }
}
