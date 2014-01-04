package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PDOSamplePairUtils {

    /**
     * Lines up the PDOSamplePairs passed in
     * from the client with the query results
     * that came from the database.
     * @param requestedPdoSamplePairs the pdo sample pairs
     *                                as it came in from the client.
     *                                @see org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderResource#getPdoSampleBillingStatus(PDOSamplePairs)
     * @param pdoSamples the pdo samples that were returned
     *                   from the database.
     *                   @see org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao#findByOrderKeyAndSampleNames(String, java.util.Set)
     * @return a new PDOSamplePairs object that has the right billing
     * status and has error fields set for pdo/sample pairs
     * that were not found.
     */
    @DaoFree
    public static PDOSamplePairs buildOutputPDOSamplePairsFromInputAndQueryResults(PDOSamplePairs requestedPdoSamplePairs, List<ProductOrderSample> pdoSamples) {
        PDOSamplePairs pdoSamplePairsResults = new PDOSamplePairs();
        for (PDOSamplePair requestedPdoSamplePair : requestedPdoSamplePairs.getPdoSamplePairs()) {
            boolean foundIt = false;
            String requestedPdoKey = requestedPdoSamplePair.getPdoKey();
            String requestedSampleName = requestedPdoSamplePair.getSampleName();
            for (ProductOrderSample pdoSample : pdoSamples) {
                if (requestedPdoKey.equals(pdoSample.getProductOrder().getBusinessKey()) && requestedSampleName.equals(pdoSample.getName())) {
                    pdoSamplePairsResults.addPdoSamplePair(requestedPdoKey,requestedSampleName,pdoSample.hasPrimaryPriceItemBeenBilled());
                    foundIt = true;
                }
            }
            if (!foundIt) {
                pdoSamplePairsResults.addPdoSamplePair(requestedPdoKey,requestedSampleName,null);
                String errorMessage = MessageFormat.format("Could not find sample {0} in PDO {1}.",requestedSampleName,requestedPdoKey);
                pdoSamplePairsResults.addError(errorMessage);
            }
        }
        return pdoSamplePairsResults;
    }

    /**
     * Converts the list of pdo/sample tuples to a map
     * where the key is the PDO key and the value is
     * a list of sample names for that PDO.
     * @param pdoSamplePairs
     * @return
     */
    @DaoFree
    public static Map<String,Set<String>> convertPdoSamplePairsListToMap(@Nonnull PDOSamplePairs pdoSamplePairs) {
        Map<String,Set<String>> pdoToSamples = new HashMap<>();
        for (PDOSamplePair pdoSamplePair : pdoSamplePairs.getPdoSamplePairs()) {
            String pdoKey = pdoSamplePair.getPdoKey();
            if (!pdoToSamples.containsKey(pdoKey)) {
                pdoToSamples.put(pdoSamplePair.getPdoKey(),new HashSet<String>());
            }
            pdoToSamples.get(pdoKey).add(pdoSamplePair.getSampleName());
        }
        return pdoToSamples;
    }
}
