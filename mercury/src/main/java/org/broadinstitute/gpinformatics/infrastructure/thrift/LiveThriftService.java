package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.ConcentrationAndVolume;
import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.LibraryData;
import edu.mit.broad.prodinfo.thrift.lims.PlateTransfer;
import edu.mit.broad.prodinfo.thrift.lims.PoolGroup;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import edu.mit.broad.prodinfo.thrift.lims.WellAndSourceTube;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Thrift service client that connects to a live thrift endpoint. All thrift
 * communication details and handling of error conditions are handled here.
 */
@Dependent
@Default
public class LiveThriftService implements ThriftService {

    private ThriftConnection thriftConnection;

    private Log log = LogFactory.getLog(this.getClass());

    /**
     * Testing only
     */
    public LiveThriftService(ThriftConnection thriftConnection, Log log) {
        this.thriftConnection = thriftConnection;
        this.log = log;
    }

    @Inject
    public LiveThriftService(ThriftConnection thriftConnection) {
        this.thriftConnection = thriftConnection;
    }

    @Override
    public TZamboniRun fetchRun(final String runName) {
        return thriftConnection.call(new ThriftConnection.Call<TZamboniRun>() {
            @Override
            public TZamboniRun call(LIMQueries.Client client) {
                try {
                    return client.fetchRun(runName);
                } catch (TZIMSException e) {
                    if (e.getDetails() != null) {
                        if (e.getDetails().contains("ZIMs failed to find run")) {
                            // this is a typical situation: the pipeline is asking for a run
                            // a tad too early, and it hasn't been registered yet, so don't panic.
                            log.info("Run " + runName + " doesn't appear to have been registered yet.  Please try again later or contact the mercury team if the problem persists.");
                        }
                        return null;
                    }
                    String message = "Failed to fetch run: " + runName;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                } catch (TException e) {
                    String message = "Failed to fetch run: " + runName;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
        });
    }

    @Override
    public TZamboniRun fetchRunByBarcode(final String runBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<TZamboniRun>() {
            @Override
            public TZamboniRun call(LIMQueries.Client client) {
                try {
                    return client.fetchRunByBarcode(runBarcode);
                } catch (TZIMSException e) {
                    if (e.getDetails() != null) {
                        if (e.getDetails().contains("ZIMs failed to find run")) {
                            // this is a typical situation: the pipeline is asking for a run
                            // a tad too early, and it hasn't been registered yet, so don't panic.
                            log.info("Run having barcode " + runBarcode + " doesn't appear to have been registered yet.  Please try again later or contact the mercury team if the problem persists.");
                        }
                        return null;
                    }
                    String message = "Failed to fetch run by barcode: " + runBarcode;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                } catch (TException e) {
                    String message = "Failed to fetch run by barcode: " + runBarcode;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                }
            }
        });
    }

    @Override
    public List<LibraryData> fetchLibraryDetailsByTubeBarcode(final List<String> tubeBarcodes, final boolean includeWorkRequestDetails) {
        return thriftConnection.call(new ThriftConnection.Call<List<LibraryData>>() {
            @Override
            public List<LibraryData> call(LIMQueries.Client client) {
                try {
                    return client.fetchLibraryDetailsByTubeBarcode(tubeBarcodes, includeWorkRequestDetails);
                } catch (TTransportException e) {
                    String exceptionMessage = e.getMessage();
                    String type;
                    switch (e.getType()) {
                        case TTransportException.ALREADY_OPEN:
                            type = "already open";
                            break;
                        case TTransportException.END_OF_FILE:
                            type = "end of file";
                            if (StringUtils.isBlank(exceptionMessage) && !includeWorkRequestDetails) {
                                // Thrift server doesn't pass in an exception message for this.
                                exceptionMessage = "Thrift requires includeWorkRequestDetails=true";
                            }
                            break;
                        case TTransportException.NOT_OPEN:
                            type = "not open";
                            break;
                        case TTransportException.TIMED_OUT:
                            type = "timed out";
                            break;
                        case TTransportException.UNKNOWN:
                            type = "unknown";
                            break;
                        default:
                            type = "unexpected error type";
                    }
                    String message = "Thrift error: " + type + ": " + exceptionMessage;
                    log.error(message, e);
                    throw new RuntimeException(message, e);
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public boolean doesSquidRecognizeAllLibraries(final List<String> barcodes) {
        return thriftConnection.call(new ThriftConnection.Call<Boolean>() {
            @Override
            public Boolean call(LIMQueries.Client client) {
                try {
                    return client.doesSquidRecognizeAllLibraries(barcodes);
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public List<String> fetchMaterialTypesForTubeBarcodes(final List<String> tubeBarcodes) {
        return thriftConnection.call(new ThriftConnection.Call<List<String>>() {
            @Override
            public List<String> call(LIMQueries.Client client) {
                try {
                    return client.fetchMaterialTypesForTubeBarcodes(tubeBarcodes);
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName) {
        return thriftConnection.call(new ThriftConnection.Call<FlowcellDesignation>() {
            @Override
            public FlowcellDesignation call(LIMQueries.Client client) {
                try {
                    return client.findFlowcellDesignationByTaskName(taskName);
                } catch (TException e) {
                    /* This seems to be thrown when the designation doesn't
                     * exist. Looking at LIMQueriesImpl.java,
                     * findFlowcellDesignationByTaskName(FcellDesignationGroup, EntityManager)
                     * is given null in this case and likely throws a
                     * NullPointerException, though that detail is not exposed
                     * to thrift clients.
                     */
                    log.error("Thrift error. Probably couldn't find designation for task name '" + taskName + "': " + e.getMessage(), e);
                    throw new RuntimeException("Designation not found for task name: " + taskName, e);
                }
            }
        });
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(final String flowcellBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<FlowcellDesignation>() {
            @Override
            public FlowcellDesignation call(LIMQueries.Client client) {
                try {
                    return client.findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find designation for flowcell barcode '" + flowcellBarcode + "': " + e.getMessage(), e);
                    throw new RuntimeException("Designation not found for flowcell barcode: " + flowcellBarcode, e);
                }
            }
        });
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByReagentBlockBarcode(final String reagentBlockBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<FlowcellDesignation>() {
            @Override
            public FlowcellDesignation call(LIMQueries.Client client) {
                try {
                    return client.findFlowcellDesignationByReagentBlockBarcode(reagentBlockBarcode);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find designation for reagent block barcode '" + reagentBlockBarcode + "': " + e.getMessage(), e);
                    throw new RuntimeException("Designation not found for flowcell barcode: " + reagentBlockBarcode);
                }
            }
        });
    }

    /**
     * Returns a list of plates that have been transferred directly into the given plate. Returns an empty list if the
     * given plate is not found.
     *
     * @param plateBarcode    the barcode of the plate to query
     * @return the immediate plate parents, or an empty list if the given plate isn't found
     */
    @Override
    public List<String> findImmediatePlateParents(final String plateBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<List<String>>() {
            @Override
            public List<String> call(LIMQueries.Client client) {
                try {
                    return client.findImmediatePlateParents(plateBarcode);
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public String fetchUserIdForBadgeId(final String badgeId) {
        return thriftConnection.call(new ThriftConnection.Call<String>() {
            @Override
            public String call(LIMQueries.Client client) {
                try {
                    return client.fetchUserIdForBadgeId(badgeId);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find user for badge ID '" + badgeId + "': " + e.getMessage(), e);
                    throw new RuntimeException("User not found for badge ID: " + badgeId, e);
                }
            }
        });
    }

    @Override
    public Map<String, Boolean> fetchParentRackContentsForPlate(final String plateBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<Map<String, Boolean>>() {
            @Override
            public Map<String, Boolean> call(LIMQueries.Client client) {
                try {
                    return client.fetchParentRackContentsForPlate(plateBarcode);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find the plate for barcode '" + plateBarcode + "': " + e.getMessage(), e);
                    throw new RuntimeException("Plate not found for barcode: " + plateBarcode, e);
                }
            }
        });
    }

    @Override
    public double fetchQpcrForTube(final String tubeBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<Double>() {
            @Override
            public Double call(LIMQueries.Client client) {
                try {
                    return client.fetchQpcrForTube(tubeBarcode);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find tube '" + tubeBarcode + "': " + e.getMessage(), e);
                    throw new RuntimeException("Tube or QPCR not found for barcode: " + tubeBarcode, e);
                }
            }
        });
    }

    @Override
    public double fetchQpcrForTubeAndType(final String tubeBarcode, final String qpcrType) {
        return thriftConnection.call(new ThriftConnection.Call<Double>() {
            @Override
            public Double call(LIMQueries.Client client) {
                try {
                    return client.fetchQpcrForTubeAndType(tubeBarcode, qpcrType);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find tube '" + tubeBarcode + "': " + e.getMessage(), e);
                    throw new RuntimeException("Tube or QPCR not found for barcode: " + tubeBarcode, e);
                }
            }
        });
    }

    @Override
    public double fetchQuantForTube(final String tubeBarcode, final String quantType) {
        return thriftConnection.call(new ThriftConnection.Call<Double>() {
            @Override
            public Double call(LIMQueries.Client client) {
                try {
                    return client.fetchQuantForTube(tubeBarcode, quantType);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find tube '" + tubeBarcode + "': " + e.getMessage(), e);
                    throw new RuntimeException("Tube or quant not found for barcode: " + tubeBarcode + ", quant type: " + quantType, e);
                }
            }
        });
    }

    @Override
    public List<LibraryData> fetchLibraryDetailsByLibraryName(final List<String> libraryNames) {
        return thriftConnection.call(new ThriftConnection.Call<List<LibraryData>>() {
            @Override
            public List<LibraryData> call(LIMQueries.Client client) {
                try {
                    return client.fetchLibraryDetailsByLibraryName(libraryNames);
                } catch (TException e) {
                    log.error("Thrift error. Probably couldn't find libraries  : " + e.getMessage(), e);
                    throw new RuntimeException("Libraries not found : " , e);
                }
            }
        });
    }

    @Override
    public List<String> fetchUnfulfilledDesignations() {
        return thriftConnection.call(new ThriftConnection.Call<List<String>>() {
            @Override
            public List<String> call(LIMQueries.Client client) {
                try {
                    return client.fetchUnfulfilledDesignations();
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public List<String> findRelatedDesignationsForAnyTube(final List<String> tubeBarcodes) {
        return thriftConnection.call(new ThriftConnection.Call<List<String>>() {
            @Override
            public List<String> call(LIMQueries.Client client) {
                try {
                    return client.findRelatedDesignationsForAnyTube(tubeBarcodes);
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public List<WellAndSourceTube> fetchSourceTubesForPlate(final String plateBarcode) {
        return thriftConnection.call(new ThriftConnection.Call<List<WellAndSourceTube>>() {
            @Override
            public List<WellAndSourceTube> call(LIMQueries.Client client) {
                try {
                    return client.fetchSourceTubesForPlate(plateBarcode);
                } catch (TTransportException e) {
                    if (e.getType() == TTransportException.END_OF_FILE) {
                        throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
                    } else {
                        throw handleThriftException(e);
                    }
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public List<PlateTransfer> fetchTransfersForPlate(final String plateBarcode, final short depth) {
        return thriftConnection.call(new ThriftConnection.Call<List<PlateTransfer>>() {
            @Override
            public List<PlateTransfer> call(LIMQueries.Client client) {
                try {
                    return client.fetchTransfersForPlate(plateBarcode, depth);
                } catch (TTransportException e) {
                    if (e.getType() == TTransportException.END_OF_FILE) {
                        throw new RuntimeException("Plate not found for barcode: " + plateBarcode);
                    } else {
                        throw handleThriftException(e);
                    }
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public List<PoolGroup> fetchPoolGroups(final List<String> tubeBarcoces) {
        return thriftConnection.call(new ThriftConnection.Call<List<PoolGroup>>() {
            @Override
            public List<PoolGroup> call(LIMQueries.Client client) {
                try {
                    return client.fetchPoolGroups(tubeBarcoces);
                } catch (TTransportException e) {
                    if (e.getType() == TTransportException.END_OF_FILE) {
                        throw new RuntimeException("Some or all of the tubes were not found.");
                    } else {
                        throw handleThriftException(e);
                    }
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    @Override
    public Map<String, ConcentrationAndVolume> fetchConcentrationAndVolumeForTubeBarcodes(final List<String> tubeBarcodes) {
        return thriftConnection.call(new ThriftConnection.Call<Map<String, ConcentrationAndVolume> >(){

            @Override
            public Map<String, ConcentrationAndVolume> call(LIMQueries.Client client) {
                try {
                    return client.fetchConcentrationAndVolumeForTubeBarcodes(tubeBarcodes);
                } catch (TTransportException e) {
                    if (e.getType() == TTransportException.END_OF_FILE) {
                        throw new RuntimeException("Some or all of the tubes were not found.");
                    } else {
                        throw handleThriftException(e);
                    }
                } catch (TException e) {
                    throw handleThriftException(e);
                }
            }
        });
    }

    private RuntimeException handleThriftException(TException e) {
        String message = "Thrift error: " + e.getMessage();
        log.error(message, e);
        return new RuntimeException(message, e);
    }
}
