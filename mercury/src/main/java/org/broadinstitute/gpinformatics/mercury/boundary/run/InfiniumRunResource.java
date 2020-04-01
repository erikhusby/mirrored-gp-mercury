package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.CrspPipelineUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JAX-RS resource for Infinium genotyping runs.
 */
@Path("/infiniumrun")
@Stateful
@RequestScoped
public class InfiniumRunResource {

    private static final Log log = LogFactory.getLog(InfiniumRunResource.class);

    /** Extract barcode, row and column from e.g. 3999595020_R12C02 */
    private static final Pattern BARCODE_PATTERN = Pattern.compile("([a-zA-Z0-9]*)_(R\\d*)(C\\d*)");
    /** This matches with attribute_definition.attribute_family in the database. */
    public static final String INFINIUM_GROUP = "Infinium";
    private static String DATA_PATH;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ControlDao controlDao;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public InfiniumRunBean getRun(@QueryParam("chipWellBarcode") String chipWellBarcode) {
        InfiniumRunBean infiniumRunBean;
        Matcher matcher = BARCODE_PATTERN.matcher(chipWellBarcode);
        if (matcher.matches()) {
            String chipBarcode = matcher.group(1);
            String row = matcher.group(2);
            String column = matcher.group(3);
            VesselPosition vesselPosition = VesselPosition.valueOf(row + column);
            LabVessel chip = labVesselDao.findByIdentifier(chipBarcode);
            if (chip == null) {
                throw new ResourceException("No such chip: " + chipBarcode, Response.Status.INTERNAL_SERVER_ERROR);
            }
            infiniumRunBean = buildRunBean(chip, vesselPosition, runDate(chip));

        } else {
            throw new ResourceException("Barcode is not of expected format", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return infiniumRunBean;
    }

    private Date runDate(LabVessel chip) {
        if (CollectionUtils.isEmpty(chip.getEvents())) {
            throw new ResourceException("No chip events found", Response.Status.INTERNAL_SERVER_ERROR);
        }
        for (LabEvent event : chip.getEvents()) {
            if (event.getLabEventType() == LabEventType.INFINIUM_XSTAIN ||
                    event.getLabEventType() == LabEventType.INFINIUM_XSTAIN_HD) {
                return event.getEventDate();
            }
        }
        return chip.getLatestEvent().getEventDate();
    }

    /**
     * Build a JAXB DTO from a chip and position (a single sample)
     */
    private InfiniumRunBean buildRunBean(LabVessel chip, VesselPosition vesselPosition, Date effectiveDate) {
        if (DATA_PATH == null) {
            DATA_PATH = ConcordanceCalculator.convertFilePaths(infiniumStarterConfig.getDataPath());
        }
        if (DATA_PATH == null) {
            throw new ResourceException("No configuration for DataPath", Response.Status.INTERNAL_SERVER_ERROR);
        }
        InfiniumRunBean infiniumRunBean;
        Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                chip.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
        if (sampleInstancesAtPositionV2.size() == 1) {
            SampleInstanceV2 sampleInstanceV2 = sampleInstancesAtPositionV2.iterator().next();
            SampleData sampleData = sampleDataFetcher.fetchSampleData(
                    sampleInstanceV2.getNearestMercurySampleName());

            ProductOrder productOrder;
            ProductOrderSample productOrderSample = sampleInstanceV2.getProductOrderSampleForSingleBucket();
            productOrder = fetchProductOrder(chip, sampleInstanceV2);

            boolean positiveControl = false;
            boolean negativeControl = false;
            if (productOrderSample == null) {
                Control processControl = evaluateAsControl(sampleData);
                if (processControl != null) {
                    if (processControl.getType() == Control.ControlType.POSITIVE) {
                        positiveControl = true;
                    } else if (processControl.getType() == Control.ControlType.NEGATIVE) {
                        negativeControl = true;
                    }
                }
            }

            GenotypingChip chipType = findChipType(productOrder, effectiveDate);

            if (chipType == null) {
                throw new ResourceException("Found no chip types for " + chip.getLabel() + " on " + effectiveDate,
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            String idatPrefix = DATA_PATH + "/" + chip.getLabel() + "/" + chip.getLabel() + "_" + vesselPosition.name();
            Map<String, String> chipAttributes = chipType.getAttributeMap();
            if (chipAttributes.isEmpty()) {
                throw new ResourceException("Found no configuration for " + chipType.getChipName(),
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            Date startDate = null;
            for (LabEvent labEvent: chip.getInPlaceLabEvents()) {
                if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_SOME_STARTED) {
                    startDate = labEvent.getEventDate();
                }
            }
            String scannerName = InfiniumRunProcessor.findScannerName(chip.getLabel(), infiniumStarterConfig);

            String batchName = null;
            if (sampleInstanceV2.getSingleBatch() != null) {
                batchName = sampleInstanceV2.getSingleBatch().getBatchName();
            }
            String productOrderId = null;
            String productName = null;
            String productFamily = null;
            String partNumber = null;
            String researchProjectId = null;
            ResearchProject.RegulatoryDesignation regulatoryDesignation = null;
            if (productOrder != null) {
                productOrderId = productOrder.getJiraTicketKey();
                productName = productOrder.getProduct().getProductName();
                productFamily = productOrder.getProduct().getProductFamily().getName();
                partNumber = productOrder.getProduct().getPartNumber();
                researchProjectId = productOrder.getResearchProject().getBusinessKey();
                regulatoryDesignation = productOrder.getResearchProject().getRegulatoryDesignation();

                //Attempt to override default chip attributes if changed in product order
                GenotypingProductOrderMapping genotypingProductOrderMapping =
                        attributeArchetypeDao.findGenotypingProductOrderMapping(productOrder.getProductOrderId());
                if (genotypingProductOrderMapping != null) {
                    for (ArchetypeAttribute archetypeAttribute : genotypingProductOrderMapping.getAttributes()) {
                        if (chipAttributes.containsKey(archetypeAttribute.getAttributeName()) &&
                                archetypeAttribute.getAttributeValue() != null) {
                            chipAttributes.put(
                                    archetypeAttribute.getAttributeName(), archetypeAttribute.getAttributeValue());
                        }
                    }
                }
            }

            String sampleLsid = sampleData.getSampleLsid();
            if (sampleLsid == null && regulatoryDesignation != null && regulatoryDesignation.isClinical()) {
                sampleLsid = CrspPipelineUtils.getCrspLSIDForBSPSampleId(sampleInstanceV2.getNearestMercurySampleName());
            }
            infiniumRunBean = new InfiniumRunBean(
                    idatPrefix + "_Red.idat",
                    idatPrefix + "_Grn.idat",
                    chipAttributes.get("illumina_manifest_unix"),
                    chipAttributes.get("manifest_location_unix"),
                    chipAttributes.get("cluster_location_unix"),
                    chipAttributes.get("zcall_threshold_unix"),
                    sampleInstanceV2.getNearestMercurySampleName(),
                    sampleData.getCollaboratorsSampleName(),
                    sampleLsid,
                    sampleData.getGender(),
                    sampleData.getPatientId(),
                    researchProjectId,
                    positiveControl,
                    negativeControl,
                    chipAttributes.get("call_rate_threshold"),
                    chipAttributes.get("gender_cluster_file"),
                    sampleData.getCollaboratorParticipantId(),
                    productOrderId,
                    productName,
                    productFamily,
                    partNumber,
                    batchName,
                    startDate,
                    scannerName,
                    chipAttributes.get("norm_manifest_unix"),
                    regulatoryDesignation == null ? null : regulatoryDesignation.name());
        } else {
            throw new RuntimeException("Expected 1 sample, found " + sampleInstancesAtPositionV2.size());
        }
        return infiniumRunBean;
    }

    /**
     * No connection to a product was found for a specific sample, so determine if it's a control.
     */
    private Control evaluateAsControl(SampleData sampleData) {
        List<Control> controls = controlDao.findAllActive();
        Control processControl = null;
        for (Control control : controls) {
            if (control.getCollaboratorParticipantId().equals(sampleData.getCollaboratorParticipantId())) {
                processControl = control;
                break;
            }
        }
        return processControl;
    }

    private GenotypingChip findChipType(ProductOrder productOrder, Date effectiveDate) {
        GenotypingChip chip = null;
        Pair<String, String> chipFamilyAndName = productEjb.getGenotypingChip(productOrder,
                effectiveDate);
        if (chipFamilyAndName.getLeft() != null && chipFamilyAndName.getRight() != null) {
            chip = attributeArchetypeDao.findGenotypingChip(chipFamilyAndName.getLeft(),
                    chipFamilyAndName.getRight());
            if (chip == null) {
                throw new ResourceException("Chip " + chipFamilyAndName.getRight() + " is not configured",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return chip;
    }

    // When the ARRAY batch is auto-created, all samples on the plate get BucketEntries, even controls (this
    // is arguably a bug in BucketEjb), so the BucketEntry can be used to determine an unambiguous PDO, unless
    // the sample pre-dates auto-creation of ARRAY batches.
    // To determine whether the sample is a process control (as opposed to a HapMap sample added to a PDO for
    // scientific purposes), we have to look for the absence of a ProductOrderSample.
    public static ProductOrder fetchProductOrder(LabVessel chip, SampleInstanceV2 sampleInstanceV2) {
        ProductOrder productOrder;
        BucketEntry singleBucketEntry = sampleInstanceV2.getSingleBucketEntry();
        ProductOrderSample productOrderSample = sampleInstanceV2.getProductOrderSampleForSingleBucket();
        if (singleBucketEntry == null) {
            if (productOrderSample == null) {
                // Likely a control, look at all samples on imported plate to try to find common ProductOrder
                TransferTraverserCriteria.VesselForEventTypeCriteria vesselForEventTypeCriteria =
                        new TransferTraverserCriteria.VesselForEventTypeCriteria(Collections.singletonList(
                                LabEventType.ARRAY_PLATING_DILUTION), true);
                chip.getContainerRole().applyCriteriaToAllPositions(vesselForEventTypeCriteria,
                        TransferTraverserCriteria.TraversalDirection.Ancestors);

                Set<ProductOrder> productOrders = new HashSet<>();
                for (Map.Entry<LabEvent, Set<LabVessel>> labEventSetEntry :
                        vesselForEventTypeCriteria.getVesselsForLabEventType().entrySet()) {
                    for (LabVessel labVessel : labEventSetEntry.getValue()) {
                        Set<SampleInstanceV2> sampleInstances = OrmUtil.proxySafeIsInstance(labVessel, PlateWell.class) ?
                                labVessel.getContainers().iterator().next().getContainerRole().getSampleInstancesV2() :
                                labVessel.getSampleInstancesV2();
                        for (SampleInstanceV2 sampleInstance : sampleInstances) {
                            ProductOrderSample platedPdoSample = sampleInstance.getProductOrderSampleForSingleBucket();
                            if (platedPdoSample != null) {
                                productOrders.add(platedPdoSample.getProductOrder());
                            }
                        }
                        if (OrmUtil.proxySafeIsInstance(labVessel, PlateWell.class)) {
                            break;
                        }
                    }
                }

                if (productOrders.size() >= 1) {
                    productOrder = productOrders.iterator().next();
                } else {
                    throw new ResourceException("Found no product orders ", Response.Status.INTERNAL_SERVER_ERROR);
                }
            } else {
                productOrder = productOrderSample.getProductOrder();
            }
        } else {
            productOrder = singleBucketEntry.getProductOrder();
        }

        return productOrder;
    }

    // Must be public, to allow calling from test
    @SuppressWarnings("WeakerAccess")
    public void setInfiniumStarterConfig(InfiniumStarterConfig infiniumStarterConfig) {
        this.infiniumStarterConfig = infiniumStarterConfig;
    }

    public void setAttributeArchetypeDao(AttributeArchetypeDao attributeArchetypeDao) {
        this.attributeArchetypeDao = attributeArchetypeDao;
    }

    static final Map<String, String> mapSerialNumberToMachineName = new HashMap<>();
    static {
        mapSerialNumberToMachineName.put("N296", "BAB");
        mapSerialNumberToMachineName.put("N0296", "BAB");
        mapSerialNumberToMachineName.put("N370", "BAA");
        mapSerialNumberToMachineName.put("N0370", "BAA");
        mapSerialNumberToMachineName.put("N700", "BAC");
        mapSerialNumberToMachineName.put("N0700", "BAC");
        mapSerialNumberToMachineName.put("N588", "BAD");
        mapSerialNumberToMachineName.put("N0588", "BAD");
        mapSerialNumberToMachineName.put("N1052", "BAF");
        mapSerialNumberToMachineName.put("N01052", "BAF");
        mapSerialNumberToMachineName.put("N1042", "BAE");
        mapSerialNumberToMachineName.put("N01042", "BAE");
    }
}
