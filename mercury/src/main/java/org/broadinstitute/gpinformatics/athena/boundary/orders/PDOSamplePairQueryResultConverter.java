package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;

import java.text.MessageFormat;
import java.util.List;

/**
 * Takes the requested pdo sample pairs and database
 * query results and creates a new pdo sample pair
 * object that can be returned to the web client
 * @see org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderResource#getPdoSampleBillingStatus(PDOSamplePairs)
 */
public class PDOSamplePairQueryResultConverter {

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
                    break;
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
}
